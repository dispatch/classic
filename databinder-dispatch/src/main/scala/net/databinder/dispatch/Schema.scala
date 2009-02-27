package net.databinder.dispatch

import scala.util.parsing.json.Parser
import scala.util.parsing.input.{Reader,StreamReader}
import java.io.{InputStream, InputStreamReader, ByteArrayInputStream}

/** Json expected typers, returning a of Some(a) casted to a type */
trait JsTypes {
  val raw_list: Option[Any] => List[Option[_]] = {
    case None => List[Option[_]]()
    case Some(l) => l.asInstanceOf[List[Option[_]]]
  }
  def list[T](t: Option[Any] => T)(a: Option[Any]) = raw_list(a).map(t)
  
  val str: (Option[Any] => String) = {
    case None => ""
    case Some(l) => l.toString
  }
  
  val num: Option[Any] => Double = {
    case None => 0.0
    case Some(l) => l.asInstanceOf[Double]
  }
  val obj: Option[Any] => Js = {
    case None => Js()
    case Some(m) => Js(m.asInstanceOf[Js#MapObj])
  }
}
  
/** Json trait builder */
trait JsDef extends JsTypes {
  case class Converter[T](s: Symbol, t: Option[Any] => T)
  implicit def sym2conv(s: Symbol) = new {
    def as[T](t: Option[Any] => T) = new Converter(s, t)
  }
  implicit def conv2m_thunk[T](c: Converter[T]) = { m: Js#MapObj => c.t(m(c.s)) }
  implicit def conv2j_thunk[T](c: Converter[T]) = { js: Js => js(c) }
  implicit def m_thunk2j_thunk[T](t: Js#MapObj => T) = { js: Js => js(t) }
  
  implicit def convs2m_thunk[T](tup: (Seq[Converter[Js]], Converter[T])): (Js#MapObj => T) = { m =>
    tup match {
      case (c :: rem, last) => c.t(m(c.s))(convs2m_thunk(rem -> last))
      case (_, last) => last.t(m(last.s)) 
    }
  }
  
}

object JsDef extends JsDef

/** Json expected value extractors, value from map with a typer applied. */
case class Js (private val base: Js#MapObj) {
  type MapObj = Map[Symbol, Option[Any]]
  def apply[T](thunk: MapObj => T): T = thunk(base)
  def apply[T](s: Symbol)(t: Option[Any] => T): T = this(b => t(b(s)))
  
  def << [T] (conv: JsDef#Converter[T])(t: T) = Js(base + (conv.s -> Some(t)))
  
  override def toString = Js.as_string(base)
}

object Js extends Parser {
  def apply(): Js = Js(Map[Symbol, Option[Any]]())
  def apply(stream: InputStream): Js = Js(process(stream))
  def apply(string: String): Js = Js(new ByteArrayInputStream(string.getBytes("UTF-8")))

  def process(input: InputStream) = 
    phrase(root)(new lexical.Scanner(StreamReader(new InputStreamReader(input)))) match {
      case Success(list: List[_], _) => mapify(list head, list tail)
      case _ => Map[Symbol, Option[Any]]()
    }

  private def mapify(tup: Any, list: List[Any]): Js#MapObj =
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
