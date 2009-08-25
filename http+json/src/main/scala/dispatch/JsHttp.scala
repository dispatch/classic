package dispatch.json

trait JsHttp extends Js {
  /** Add JSON-processing method ># to dispatch.Request */
  implicit def Request2JsonRequest(r: Request) = new JsonRequest(r)
  /** Add String conversion since Http#str2req implicit will not chain. */
  implicit def String2JsonRequest(r: String) = new JsonRequest(new Request(r))

  class JsonRequest(r: Request) {
    /** Process response as JsValue in block */
    def ># [T](block: json.Js.JsF[T]) = r >> { stm => block(json.Js(stm)) }
  }
}
object JsHttp extends JsHttp
