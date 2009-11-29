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
  implicit def sym2op(sym: Symbol) = new {
    def ?[T](block: JValue => List[T]): JValue => List[T] = {
      case JObject(l) => l filter { _.name == sym.name } flatMap { jf => block(jf.value) }
      case _ => Nil
    }
  }
  val str: (JValue => List[String]) = {
    case JString(s) => s :: Nil
    case _ => Nil
  }
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
}