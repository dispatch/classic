package dispatch.json

import scala.util.parsing.json.Parser
import scala.util.parsing.input.{Reader,StreamReader}
import java.io.{InputStream, InputStreamReader, ByteArrayInputStream}

trait Js {
  type M = Map[Symbol, Option[Any]]
  implicit val ctx: Option[Obj] = None
  trait Extract[T] {
    def unapply(js: Js#M): Option[T]
  }
  case class Rel[T](parent: Option[Obj], child: Extract[T]) extends Extract[T] {
    def unapply(js: Js#M) = parent match {
      case Some(parent) => js match {
        case parent(child(t)) => Some(t)
      }
      case None => js match {
        case child(t) => Some(t)
      }
    }
  }
  type Cast[T] = Option[_] => Option[T]
  def cast[T](cl: Class[T]): Cast[T] = {
    case Some(v) if cl.isInstance(v) => Some(v.asInstanceOf[T])
    case _ => None
  }
  val str = cast(classOf[String])
  val num = cast(classOf[Number])
  val obj = cast(classOf[Js#M])
  val list = cast(classOf[List[Option[_]]])
  def list[T](c2: Cast[T]): Cast[List[T]] = list andThen { _ map { _.map(e => c2(e).get) } }
  case class Basic[T](sym: Symbol, cst: Cast[T]) extends Extract[T] {
    def unapply(js: Js#M) = js.get(sym) flatMap cst
  }
  class Obj(sym: Symbol)(implicit parent: Option[Obj]) 
      extends Rel[Js#M](parent, Basic(sym, obj)) {
    implicit val ctx = Some(this)
  }
  implicit def ext2fun[T](ext: Extract[T]): M => Option[T] =  ext.unapply
  implicit def sym2rel[T](sym: Symbol) = new {
    def ? [T](cst: Cast[T])(implicit parent: Option[Obj]) = 
      new Rel(parent, Basic(sym, cst))
    def ! [T](cst: Cast[T])(implicit parent: Option[Obj]) = 
      new Rel(parent, Basic(sym, cst)).unapply _ andThen { _.get }
  }
}

/*case class Converter[T](s: Symbol, t: Option[Any] => T) {
  def :: [T](co: Converter[Js]) = ConverterChain(co :: Nil, this)
  def << [T] (t: T): Js#M => Js = { m => Js(m + (s -> Some(t))) }
} */

object Js extends Parser with Js {

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
