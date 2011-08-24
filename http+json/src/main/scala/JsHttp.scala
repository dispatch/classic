package dispatch.json
import dispatch._

trait ImplicitJsHandlers {
  /** Add JSON-processing method ># to dispatch.Request */
  implicit def handlerToJsHandlers(r: HandlerVerbs) = new JsHandlers(r)
  implicit def requestToJsHandlers(r: Request) = new JsHandlers(r)
  implicit def stringToJsHandlers(r: String) = new JsHandlers(new Request(r))
}

trait JsHttp extends ImplicitJsHandlers with Js

object JsHttp extends JsHttp

class JsHandlers(subject: HandlerVerbs) {
  /** Process response as JsValue in block */
  def ># [T](block: json.Js.JsF[T]) = subject >- { s =>
    block(json.Js(s))
  }
}
