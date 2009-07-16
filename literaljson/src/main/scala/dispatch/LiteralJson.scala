package dispatch.litjson

import literaljson.JsonAST._
import literaljson.JsonParser._

object Js {
  /** Add JSON-processing method ># to dispatch.Request */
  implicit def Request2JsonRequest(r: dispatch.Request) = new JsonRequest(r)

  class JsonRequest(r: Request) {
    /** Process response as JsValue in block */
    def ># [T](block: JValue => T) = r >- { s => block(parse(s) match {
      case Left(err) => error(err.message)
      case Right(jvalue) => jvalue
    }) }
  }
}
