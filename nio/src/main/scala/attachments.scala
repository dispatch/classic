package dispatch.nio

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.protocol._
import org.apache.http.nio.{ContentDecoder,IOControl}
import org.apache.http.nio.entity.NStringEntity
import org.apache.http.message.BasicHttpEntityEnclosingRequest

trait RequestAttachment {
  /** If the request contains a string body we can convert, do it */
  private def nioize(req: HttpRequest) = {
    req match {
      case req: BasicHttpEntityEnclosingRequest =>
        req.getEntity match {
          case ref: dispatch.RefStringEntity =>
            val ent = new NStringEntity(ref.string, ref.charset)
            ent.setContentType(ref.getContentType)
            req.setEntity(ent)
          case ent => ()
        }
      case req => ()
    }
  }
  def request: HttpRequest
  nioize(request)
}

case class IOFuture[T](request: HttpRequest, block: HttpResponse => T) 
    extends Function0[T] with RequestAttachment {
  private val result_q = new java.util.concurrent.ArrayBlockingQueue[T](1)
  private var result: Option[T] = None
  def isSet = result.isDefined
  private [nio] def response_ready(res: HttpResponse) {
    result_q.put(block(res))
  }
  def apply(): T = {
    this.synchronized {
      result.getOrElse {
        val r = result_q.take()
        result = Some(r)
        r
      }
    }
  }
}

case class IOCallback(request: HttpRequest, callback: dispatch.Callback)
     extends RequestAttachment {
  def with_decoder(response: HttpResponse, decoder: ContentDecoder) {
    val buffer = java.nio.ByteBuffer.allocate(Http.socket_buffer_size)
    val length = decoder.read(buffer)
    if (length > 0)
      callback.function(response, buffer.array(), length)
  }
}
