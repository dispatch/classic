package dispatch

import org.apache.http.HttpResponse

object Callback {
  type Function = (HttpResponse, Array[Byte], Int) => Unit
  type Finish[T] = HttpResponse => T

  def strings[T](req: Request, block: (String => Unit), finish: Finish[T]) = Callback(
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
   * only split on the divider, and handles any leftovers in finish. Skips
   * empty strings. */
  def stringsBy[T](divider: String)
                  (req: Request, block: (String => Unit), finish: Finish[T]) = {
    var buffer = ""
    strings(
      req,
      { string =>
        val strings = (buffer + string).split(divider, -1)
        strings.take(strings.length - 1).filter { !_.isEmpty }.foreach(block)
        buffer = strings.last
      }, 
      { res =>
        if (!buffer.isEmpty) block(buffer)
        finish(res)
      }
    )
  }
  /** callback transformer for strings split on the newline character, newline removed */
  def lines[T] = stringsBy[T]("[\n\r]+")_
}

case class Callback[T](request: Request, 
                       function: Callback.Function, 
                       finish: Callback.Finish[T])

trait ImplicitCallbackVerbs {
  implicit def toCallbackVerbs(req: Request) = new CallbackVerbs(req)
  implicit def stringToCallbackVerbs(str: String) = new CallbackVerbs(new Request(str))
}
object CallbackVerbs extends ImplicitCallbackVerbs

class CallbackVerbs(subject: Request) {
  import Callback._
  val nf: Callback.Finish[Unit] = _ => ()
  def ^[T](callback: Function, finish: Finish[T] = nf) =
    Callback(subject, callback, finish)
  def ^-[T](callback: (String => Unit), finish: Finish[T] = nf) =
    strings(subject, callback, finish)
  /** strings split on the newline character, newline removed */
  def ^--[T](callback: (String => Unit), finish: Finish[T] = nf) =
    lines(subject, callback, finish)
}
