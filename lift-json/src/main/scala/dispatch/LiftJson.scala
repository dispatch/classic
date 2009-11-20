package dispatch.liftjson
import dispatch._

import net.liftweb.json._

object Js {
  /** Add JSON-processing method ># to dispatch.Request */
  implicit def Request2JsonRequest(r: dispatch.Request) = new JsonRequest(r)
  /** Add String conversion since Http#str2req implicit will not chain. */
  implicit def String2JsonRequest(r: String) = new JsonRequest(new Request(r))

  class JsonRequest(r: Request) {
    /** Process response as JsValue in block */
    def ># [T](block: JsonAST.JValue => T) = r >- { s => block(JsonParser.parse(s)) }
  }
}
