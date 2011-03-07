package dispatch

import org.apache.http.HttpResponse
import util.control.Exception._

object Callback {
  type Function = (HttpResponse, Array[Byte], Int) => Unit
  type Finish[T] = HttpResponse => T

  def apply[T](request: Request, 
               function: Callback.Function, 
               finish: Callback.Finish[T]): Callback[T] =
    Callback(request, function, finish, nothingCatcher)
                  

  def apply(request: Request, function: Callback.Function): Callback[Unit] = 
    Callback(request, function, { _ => () })

  def strings[T](req: Request, block: (String => Unit)) = {
    @volatile var charset: Option[String] = None
    Callback(
      req,
      (res, bytes, len) => {
        charset orElse {
          charset = (for {
            ct <- res.getHeaders("Content-Type").headOption
            elem <- ct.getElements.headOption
            param <- Option(elem.getParameterByName("charset"))
          } yield param.getValue()) orElse Some(Request.factoryCharset)
          charset
        } map { cs => block(new String(bytes, 0, len, cs)) }
      }
    )
  }

  /** Divide input up by given regex. Buffers across inputs so strings are
   * only split on the divider, and handles any leftovers in finish. Skips
   * empty strings. */
  def stringsBy[T](divider: String)
                  (req: Request, block: (String => Unit)) = {
    var buffer = ""
    strings(
      req,
      { string =>
        val strings = (buffer + string).split(divider, -1)
        strings.take(strings.length - 1).filter { !_.isEmpty }.foreach(block)
        buffer = strings.last
      }
    ) ^> { res => 
      if (!buffer.isEmpty) block(buffer)
    }
  }
  /** callback transformer for strings split on the newline character, newline removed */
  def lines[T] = stringsBy[T]("[\n\r]+")_
}

case class Callback[T](request: Request, 
                  function: Callback.Function, 
                  finish: Callback.Finish[T],
                  catcher: Catcher[T]) {
  def ^> [T](finish: Callback.Finish[T]) = Callback(request, function, { res =>
    this.finish(res)
    finish(res)
  })
}

trait ImplicitCallbackVerbs {
  implicit def toCallbackVerbs(req: Request) = new CallbackVerbs(req)
  implicit def stringToCallbackVerbs(str: String) = new CallbackVerbs(new Request(str))
}
object CallbackVerbs extends ImplicitCallbackVerbs

class CallbackVerbs(subject: Request) {
  import Callback._
  def ^[T](callback: Function) = Callback(subject, callback)
  def ^-[T](callback: String => Unit) = strings(subject, callback)
  /** strings split on the newline character, newline removed */
  def ^--[T](callback: String => Unit) = lines(subject, callback)
}
