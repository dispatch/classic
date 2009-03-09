package dispatch.json

import scala.util.parsing.json.Parser
import scala.util.parsing.input.{Reader,StreamReader}
import java.io.InputStream

trait Js {
  type M 
  implicit val ctx: Option[Obj] = None
  trait Extract[T] {
    def unapply(js: JsValue): Option[T]
  }
  case class Rel[T](parent: Option[Obj], child: Extract[T]) extends Extract[T] {
    def unapply(js: JsValue) = parent match {
      case Some(parent) => js match {
        case parent(child(t)) => Some(t)
      }
      case None => js match {
        case child(t) => Some(t)
      }
    }
  }
  type Cast[T] = JsValue => Option[T]
  val str: Cast[String] = {
    case JsString(v) => Some(v)
    case _ => None
  }
  val num: Cast[BigDecimal] = {
    case JsNumber(v) => Some(v)
    case _ => None
  }
  val bool: Cast[Boolean] = {
    case JsBoolean(v) => Some(v)
    case _ => None
  }
  // keep in wrapped type to allow nested extractors
  val obj: Cast[JsObject] = {
    case JsObject(v) => Some(JsObject(v))
    case _ => None
  }
  val list: Cast[List[JsValue]] = {
    case JsArray(v) => Some(v)
    case _ => None
  }
  def list[T](c2: Cast[T]): Cast[List[T]] = list andThen { _ map { _.map(e => c2(e).get) } }
  case class Basic[T](sym: Symbol, cst: Cast[T]) extends Extract[T] {
    def unapply(js: JsValue) = js match {
      case js: JsObject => js.self.get(JsString(sym)) flatMap cst
      case _ => None
    }
  }
  class Obj(sym: Symbol)(implicit parent: Option[Obj]) 
      extends Rel[JsObject](parent, Basic(sym, obj)) {
    implicit val ctx = Some(this)
  }
  implicit def ext2fun[T](ext: Extract[T]): JsValue => Option[T] =  ext.unapply
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

  //def apply(): JsValue = Map[Symbol, Option[Any]]()
  def apply(stream: InputStream): JsValue = JsValue.fromStream(stream)
  def apply(string: String): JsValue = JsValue.fromString(string)


}
