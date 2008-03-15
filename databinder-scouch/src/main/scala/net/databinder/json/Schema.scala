package net.databinder.json

trait Schema {
  def loc(base: Option[Map[Symbol, Option[Any]]], sub_sym: Symbol) = 
    (base flatMap { _(sub_sym) }).asInstanceOf[Option[Map[Symbol, Option[Any]]]]

  case class String(symbol: Symbol) extends Value[java.lang.String](symbol)

  case class Int(symbol: Symbol) extends Value[scala.Int](symbol)

  case class List[T](symbol: Symbol) extends Value[scala.List[T]](symbol)

  case class Object(symbol: Symbol) extends Value[Map[Symbol, Option[Any]]](symbol) with Schema {
    override def loc(base: Option[Map[Symbol, Option[Any]]], sub_sym: Symbol) = 
      super.loc(loc(base), sub_sym)
  }

  class Value[T](val sym: Symbol) {
    def loc(base: Option[Map[Symbol, Option[Any]]]) = 
      Schema.this.loc(base, sym).asInstanceOf[Option[T]]
  }
}

import scala.util.parsing.json.Parser
import scala.util.parsing.input.{Reader,StreamReader}
import java.io.{InputStream, InputStreamReader}

class Store (val base: Map[Symbol, Option[Any]]){
  def this(stream: InputStream) = this(Store process stream)

  def apply[T](ref: Schema#Value[T]) = (ref loc Some(base))
  
/*  def << [T](ref: JsValue[T])(value: Any): Store = 
    new Store(base + (ref.sym.name -> (value match {
      case opt: Option[_] => opt
      case value => Some(value)
    }))) */
}

object Store extends Parser {
  def process(input: InputStream) = 
    phrase(root)(new lexical.Scanner(StreamReader(new InputStreamReader(input)))) match {
      case Success(list: List[_], _) => mapify(list head, list tail)
      case _ => Map[Symbol, Option[Any]]()
    }

  private def mapify(tup: Any, list: List[Any]): Map[Symbol, Option[Any]] =
    (list match {
      case Nil => Map[Symbol, Option[Any]]()
      case _ => mapify(list head, list tail)
    }) + (tup match { case (key: String, value) => Symbol(key) -> resolve(value) })

  def listify(value: Any, list: List[Any]): List[Any] = 
    resolve(value) :: (list match {
      case Nil => Nil
      case list => listify(list head, list tail)
    })

  def resolve(value: Any) = value match {
    case list: List[_] => Some(list.head match {
      case tup: (_, _) => mapify(tup, (list tail))
      case value => listify(value, list tail)
    })
    case null => None
    case value => Some(value)
  }
}