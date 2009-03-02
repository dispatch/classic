package net.databinder.dispatch

import scala.util.parsing.json.Parser
import scala.util.parsing.input.{Reader,StreamReader}
import java.io.{InputStream, InputStreamReader, ByteArrayInputStream}

/** Json expected typers, returning a of Some(a) casted to a type */
trait JsType {
  trait Extract[T] {
    def unapply(js: Js#M): Option[T]
  }
  trait Extractor[T] extends Extract[T] {
    val sym: Symbol
    def unapply(js: Js#M) = js(sym) map { _.asInstanceOf[T] }
  }
  case class Str(sym: Symbol) extends Extractor[String]
  case class Num(sym: Symbol) extends Extractor[Double]
  case class Obj(sym: Symbol) extends Extractor[Js#M] {
    def << [T](e: Extract[T]) = Rel(this, e)
  }
  case class Rel[T](parent: Obj, child: Extract[T]) extends Extract[T] {
    def unapply(js: Js#M) = js match {
      case parent(child(v)) => Some(v)
      case _ => None
    }
  }
  case class RawList(sym: Symbol) extends Extractor[List[Option[_]]]
  case class Lst[T](sub: Extractor[T]) extends Extract[List[T]] {
    val list = RawList(sub.sym)
    def unapply(js: Js#M) = js match {
      case list(l) => Some(l map {
        case Some(e) => e.asInstanceOf[T]
        case None => null.asInstanceOf[T]
      })
    }
  }
}

/*case class Converter[T](s: Symbol, t: Option[Any] => T) {
  def :: [T](co: Converter[Js]) = ConverterChain(co :: Nil, this)
  def << [T] (t: T): Js#M => Js = { m => Js(m + (s -> Some(t))) }
} */


/** Json expected value extractors, value from map with a typer applied. */
trait Js extends JsType {
  type M = Map[Symbol, Option[Any]]

  implicit def ext2fun[T](ext: JsType#Extract[T]): M => T = {
    case ext(t) => t
  }
}

object Js extends Parser {

  def apply(): Js#M = Map[Symbol, Option[Any]]()
  def apply(stream: InputStream): Js#M = process(stream)
  def apply(string: String): Js#M = Js(new ByteArrayInputStream(string.getBytes("UTF-8")))

  def process(input: InputStream) = 
    phrase(root)(new lexical.Scanner(StreamReader(new InputStreamReader(input)))) match {
      case Success(list: List[_], _) => mapify(list head, list tail)
      case _ => Map[Symbol, Option[Any]]()
    }

  private def mapify(tup: Any, list: List[Any]): Js#M =
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
      case value: String => qt(value.replace("\"", "\\\""))
      case value => value.toString
    }
  }
}
