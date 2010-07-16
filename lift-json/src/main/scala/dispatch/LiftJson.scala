package dispatch.liftjson
import dispatch._

import net.liftweb.json._
import JsonAST._
import JsonDSL._

import java.util.Date

object Js extends TypeMappers {
  /** Add JSON-processing method ># to dispatch.Request */
  implicit def Request2JsonRequest(r: dispatch.Request) = new JsonRequest(r)
  /** Add String conversion since Http#str2req implicit will not chain. */
  implicit def String2JsonRequest(r: String) = new JsonRequest(new Request(r))

  class JsonRequest(r: Request) {
    /** Process response as JsValue in block */
    def ># [T](block: JValue => T) = r >- { s => block(JsonParser.parse(s)) }
    def as_pretty = ># { js => pretty(render(js))}
  }
  implicit def jvlistcomb[LT](block: JValue => List[LT]) = new JvListComb(block)
  class JvListComb[LT](block: JValue => List[LT]) {
    /** Synonym for Function1#andThen */
    def ~>[T](next: List[LT] => T) = block andThen next
    /** @return a function mapping next over block's output */
    def >~>[T](next: LT => T) = ~> { _ map next }
    /** @return a function flat-mapping next over block's output */
    def >>~>[T](next: LT => List[T]) = ~> { _ flatMap next }
  }
  implicit def jvcomb[T](block: JValue => T) = new JvComb(block)
  class JvComb[T](block: JValue => T) {
    /** @return function that returns a tuple of block and other's output */
    def ~ [O](other: JValue => O) = { jv: JValue => (block(jv), other(jv)) }
  }
  implicit def jvbind[A <: JValue](list: List[A]) = new JvBind(list)
  class JvBind[A <: JValue](list: List[A]) {
    /** @return synonym for flatMap (bind in Haskell) */
    def >>=[B](f: A => Iterable[B]) = list.flatMap(f)
  }
  implicit def sym2op(sym: Symbol) = new SymOp(sym)
  class SymOp(sym: Symbol) {
    def ?[T](block: JValue => List[T]): JValue => List[T] = {
      case JObject(l) => l filter { _.name == sym.name } flatMap { jf => block(jf.value) }
      case JField(name, value) if name == sym.name => block(value) 
      case _ => Nil
    }
  }
}
trait TypeMappers {
  import Js._
  val str: (JValue => List[String]) = {
    case JString(s) => s :: Nil
    case _ => Nil
  }
  val int: (JValue => List[BigInt]) = {
    case JInt(i) => i :: Nil
    case _ => Nil
  }
  val date = int >~> { millis => new Date(millis.intValue) }
  def datestr(format: String) = {
    val df = new java.text.SimpleDateFormat(format)
    str >>~> { ds => try { df.parse(ds) :: Nil } catch {
      case e: java.text.ParseException => Nil
    } }
  }
  val double: (JValue => List[Double]) = {
    case JDouble(d) => d :: Nil
    case _ => Nil
  }
  val bool: (JValue => List[Boolean]) = {
    case JBool(b) => b :: Nil
    case _ => Nil
  }
  val obj: (JValue => List[JField]) = {
    case JObject(l) => l
    case _ => Nil
  }
  val ary: (JValue => List[JValue]) = {
    case JArray(l) => l
    case _ => Nil
  }
  def in[T <: JValue](values: T*): (JValue => List[T]) = { value =>
    values filter { _ == value } toList
  }
  class Obj(name: Symbol) extends (JValue => List[JField]) {
    def apply(jv: JValue) = (name ? obj)(jv)
  }
}
