package dispatch

import org.apache.http.HttpResponse

object Callback {
  type Function = (HttpResponse, Array[Byte], Int) => Unit
  type Finish = HttpResponse => Unit

  def strings(req: Request, block: (String => Unit), finish: Finish) = Callback(
    req,
    (res, bytes, len) => {
      val charset = for {
        ct <- res.getHeaders("Content-Type").headOption
        elem <- ct.getElements.headOption
        param <- Option(elem.getParameterByName("charset"))
      } yield param.getValue()
      block(new String(bytes, 0, len, charset.getOrElse(Request.factoryCharset)))
    },
    finish
  )

  /** Divide input up by given regex. Buffers across inputs so strings are
   * only split on the divider, and handles any leftovers in finish. */
  def stringsBy(divider: String)
               (req: Request, block: (String => Unit), finish: Finish) = {
    var buffer = ""
    val len = divider.length
    strings(
      req,
      { string =>
        val strings = (buffer + string).split(divider, -1)
        strings.take(strings.length - 1).foreach(block)
        buffer = strings.last
      }, 
      { res =>
        if (!buffer.isEmpty) block(buffer)
        finish(res)
      }
    )
  }
  /** callback transformer for strings split on the newline character, newline removed */
  val lines = stringsBy("\n")_
}

case class Callback(request: Request, 
                    function: Callback.Function, 
                    finish: Callback.Finish)

trait ImplicitCallbackVerbs {
  implicit def toCallbackVerbs(req: Request) = new CallbackVerbs(req)
}
object CallbackVerbs extends ImplicitCallbackVerbs

class CallbackVerbs(subject: Request) {
  import Callback._
  val nf: Callback.Finish = _ => ()
  def ^(callback: Function, finish: Finish = nf) =
    Callback(subject, callback, finish)
  def ^-(callback: (String => Unit), finish: Finish = nf) =
    strings(subject, callback, finish)
  /** strings split on the newline character, newline removed */
  def ^--(callback: (String => Unit), finish: Finish = nf) =
    lines(subject, callback, finish)
}
