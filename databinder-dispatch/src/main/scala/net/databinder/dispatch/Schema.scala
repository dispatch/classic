package net.databinder.dispatch

trait Schema {
  def loc(base: Option[Map[Symbol, Option[Any]]], sub_sym: Symbol) = 
    (base flatMap { _(sub_sym) })

  def replace(base: Option[Map[Symbol, Option[Any]]], sub_sym: Symbol, value: Option[Any]) = 
    (base map { _ + (sub_sym -> value) })

  case class String(symbol: Symbol) extends Value[java.lang.String](symbol)

  case class Int(symbol: Symbol) extends Value[scala.Int](symbol)

  case class List[T](symbol: Symbol) extends Value[scala.List[T]](symbol)

  case class Object(symbol: Symbol) extends Value[Map[Symbol, Option[Any]]](symbol) with Schema {
    override def loc(base: Option[Map[Symbol, Option[Any]]], sub_sym: Symbol) = 
      super.loc(loc(base), sub_sym)
    override def replace(base: Option[Map[Symbol, Option[Any]]], sub_sym: Symbol, value: Option[Any]) = 
      replace(base, super.replace(loc(base), sub_sym, value))
  }

  class Value[T](val sym: Symbol) {
    def loc(base: Option[Map[Symbol, Option[Any]]]) = 
      Schema.this.loc(base, sym).asInstanceOf[Option[T]]

    def replace(base: Option[Map[Symbol, Option[Any]]], value: Option[T]) = 
      Schema.this.replace(base, sym, value)
  }
}

import scala.util.parsing.json.Parser
import scala.util.parsing.input.{Reader,StreamReader}
import java.io.{InputStream, InputStreamReader}

class Store (val base: Map[Symbol, Option[Any]]){
  def this(stream: InputStream) = this(Store process stream)

  def apply[T](ref: Schema#Value[T]) = (ref loc Some(base))
  
  def << [T](ref: Schema#Value[T])(value: Option[T]): Store = 
    new Store(ref.replace(Some(base), value).get)

  override def toString = Store.as_string(base)
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

  private def listify(value: Any, list: List[Any]): List[Any] = 
    resolve(value) :: (list match {
      case Nil => Nil
      case list => listify(list head, list tail)
    })

  private def resolve(value: Any) = value match {
    case list: List[_] => Some(list.head match {
      case tup: (_, _) => mapify(tup, (list tail))
      case value => listify(value, list tail)
    })
    case null => None
    case value => Some(value)
  }
  
  private def qt(str: String) = "\"" + str + "\""

  private def as_string(obj: Map[Symbol,Option[Any]]): String =
    "{" + (obj map { tup => qt(tup._1.name) + ":" + as_string(tup._2) }).mkString(",") + "}"

  private def as_string(obj: List[Option[Any]]): String =
    "[" + (obj map as_string ).mkString(",") + "]"

  private def as_string(value: Option[Any]): String = value match {
    case None => "null"
    case Some(value) => value match {
      case value: Map[_, _] => as_string(value.asInstanceOf[Map[Symbol,Option[Any]]])
      case value: List[_] => as_string(value.asInstanceOf[List[Option[Any]]])
      case value: String => qt(value.toString)
      case value => value.toString
    }
  }
}