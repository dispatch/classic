package dispatch.json
import dispatch._

trait ImplicitJsHandlers {
  /** Add JSON-processing method ># to dispatch.Request */
  implicit def requestToJsHandlers(r: Request) = new JsHandlers(r)
  implicit def stringToJsHandlers(r: String) = new JsHandlers(new Request(r))
}

trait JsHttp extends ImplicitJsHandlers with Js

object JsHttp extends JsHttp

class JsHandlers(subject: Request) {
  /** Process response as JsValue in block */
  def ># [T](block: json.Js.JsF[T]) = subject >> { stm => block(json.Js(stm)) }
}
