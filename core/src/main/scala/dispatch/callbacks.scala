package dispatch

import org.apache.http.HttpResponse

object Callback {
  type Function = (HttpResponse, Array[Byte], Int) => Unit

  def strings(andThen: (String => Unit)): Function = (res, bytes, len) => {
    val charset = for {
      ct <- res.getHeaders("Content-Type").headOption
      elem <- ct.getElements.headOption
      param <- Option(elem.getParameterByName("charset"))
    } yield param.getValue()
    andThen(new String(bytes, 0, len, charset.getOrElse(Request.factoryCharset)))
  }

  def stringsBy(divider: String)(andThen: (String => Unit)): Function = {
    var buffer = ""
    val len = divider.length
    strings { string =>
      val idx = string.indexOf(divider)
      if (idx > 0) {
        val out = buffer + string.substring(0, idx)
        buffer = string.substring(idx + len)
        andThen(out)
      } else {
        buffer = buffer + string
      }
    }
  }
  /** callback transformer for strings split on the newline character, newline removed */
  val lines = stringsBy("\n")_
}
