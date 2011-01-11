package dispatch.nio

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.message.BasicHttpEntityEnclosingRequest
import org.apache.http.protocol._
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.nio.entity.NStringEntity
import org.apache.http.nio.{ContentDecoder,IOControl}
import java.net.InetSocketAddress

class Http extends dispatch.HttpExecutor {
  val http_params = make_params
  def make_params =
    (new org.apache.http.params.SyncBasicHttpParams)
      .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
      .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
      .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
      .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
      .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)

  def execution_handler =
    new org.apache.http.nio.protocol.NHttpRequestExecutionHandler {
      val request_response = "request_response"
      def submitRequest(ctx: HttpContext) = {
        val flag = "request_submitted"
        if (ctx.getAttribute(flag) == null) {
          ctx.setAttribute(flag, true)
          ctx.getAttribute(request_response) match {
            case IOFuture(req: HttpRequest, _) => req
            case _ => error("request_response of wrong type")
          }
        } else null
      }
      def finalizeContext(ctx: HttpContext) { }
      def handleResponse(response: HttpResponse, ctx: HttpContext) {
        ctx.getAttribute(request_response) match {
          case fut: IOFuture[_] => fut.response_ready(response)
          case _ => ()
        }
      }
      def initalizeContext(ctx: HttpContext, attachment: Any) {
        ctx.setAttribute(request_response, attachment)
      }
      def responseEntity(res: HttpResponse, ctx: HttpContext) =
        ctx.getAttribute(request_response) match {
          case callback: IOCallback => new CallbackEntity {
            def consumeContent(decoder: ContentDecoder, ioc: IOControl) {
              callback.block(decoder)
            }
            def finish() { }
          }
          case _ => new org.apache.http.nio.entity.BufferingNHttpEntity(
            res.getEntity, new org.apache.http.nio.util.HeapByteBufferAllocator)
        }
    }

  def http_handler =
    new org.apache.http.nio.protocol.AsyncNHttpClientHandler(
      new ImmutableHttpProcessor(Array(
        new RequestContent(),
        new RequestTargetHost(),
        new RequestConnControl(),
        new RequestUserAgent(),
        new RequestExpectContinue()
      )),
      execution_handler,
      new org.apache.http.impl.DefaultConnectionReuseStrategy,
      http_params
    )

  def worker_count = 4
  val io_reactor = new DefaultConnectingIOReactor(worker_count, http_params)
  val reactor_thread = start_reactor_thread
  def start_reactor_thread =
    (new Thread(new Runnable {
      def run { 
        io_reactor.execute(new DefaultClientIOEventDispatch(http_handler, http_params))
      }
    })).start()
  
  def execute[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                 req: HttpRequest, block: HttpResponse => T): HttpPackage[T] = {
    nioize(req)
    val future = IOFuture(req, block)
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      io_reactor.connect(
        new InetSocketAddress(
          host.getHostName, 
          if (host.getPort == -1) 80 else host.getPort),
        null,
        future,
        null
      )
      future
    }
  }
  type HttpPackage[T] = dispatch.futures.Futures.Future[T]
  /** If the request contains a string body we can convert, do it */
  private def nioize(req: HttpRequest) {
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
  def shutdown() {
    io_reactor.shutdown()
  }
}
case class IOFuture[T](request: HttpRequest, block: HttpResponse => T) extends Function0[T] {
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

case class IOCallback(request: HttpRequest, block: (ContentDecoder => Unit))

trait CallbackEntity extends org.apache.http.nio.entity.ConsumingNHttpEntity {
  import org.apache.http.nio._

  def consumeContent() { }
  def getContent = { error("io stream not supported") }
  def getContentEncoding = null
  def getContentLength = -1
  def getContentType = null
  def isRepeatable = false
  def isStreaming = true
  def isChunked = true
  def writeTo(str: java.io.OutputStream) { error("io stream not supported") }
}
