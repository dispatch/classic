package dispatch.liftjson
import dispatch._

import net.liftweb.json._
import JsonAST._
import JsonDSL._

object Js {
  /** Add JSON-processing method ># to dispatch.Request */
  implicit def Request2JsonRequest(r: dispatch.Request) = new JsonRequest(r)
  /** Add String conversion since Http#str2req implicit will not chain. */
  implicit def String2JsonRequest(r: String) = new JsonRequest(new Request(r))

  class JsonRequest(r: Request) {
    /** Process response as JsValue in block */
    def ># [T](block: JValue => T) = r >- { s => block(JsonParser.parse(s)) }
    def as_pretty = ># { js => pretty(render(js))}
  }
  val str: (JValue => List[String]) = {
    case JString(s) => s :: Nil
    case _ => Nil
  }
  val date = str // todo
  val int: (JValue => List[BigInt]) = {
    case JInt(i) => i :: Nil
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
  implicit def jvlistcomb(block: JValue => List[JValue]) = new JvListComb(block)
  class JvListComb(block: JValue => List[JValue]) {
    /** Synonym for Function1#andThen */
    def ~>[T](next: List[JValue] => T) = block andThen next
    /** @return a function mapping next over block's output */
    def >~>[T](next: JValue => T) = ~> { _ map next }
    /** @return a function flat-mapping next over block's output */
    def >>~>[T](next: JValue => List[T]) = ~> { _ flatMap next }
  }
  implicit def jvcomb[T](block: JValue => T) = new JvComb(block)
  class JvComb[T](block: JValue => T) {
    /** @return function that returns a tuple of block and other's output */
    def ~ [O](other: JValue => O) = { jv: JValue => (block(jv), other(jv)) }
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
