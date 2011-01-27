package dispatch.nio

import dispatch.Callback
import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity,HttpException}
import org.apache.http.message.BasicHttpEntityEnclosingRequest
import org.apache.http.protocol._
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.nio.{ContentDecoder,IOControl,NHttpConnection}
import java.net.InetSocketAddress
import java.io.IOException

object Http {
  val socket_buffer_size = 8 * 1024
}

class Http extends dispatch.HttpExecutor {
  val http_params = make_params
  def make_params =
    (new org.apache.http.params.SyncBasicHttpParams)
      .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 10000)
      .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
      .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, Http.socket_buffer_size)
      .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
      .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)

  def execution_handler =
    new org.apache.http.nio.protocol.NHttpRequestExecutionHandler {
      val request_attachment = "request_attachment"
      def submitRequest(ctx: HttpContext) = {
        val flag = "request_submitted"
        if (ctx.getAttribute(flag) == null) {
          ctx.setAttribute(flag, true)
          ctx.getAttribute(request_attachment) match {
            case att: RequestAttachment => att.request
            case _ => error("request_attachment of wrong type")
          }
        } else null
      }
      def finalizeContext(ctx: HttpContext) { }
      def handleResponse(res: HttpResponse, ctx: HttpContext) {
        ctx.getAttribute(request_attachment) match {
          case fut: IOFuture[_] => fut.response_ready(res)
          case ioc: IOCallback => ioc.callback.finish(res)
          case _ => ()
        }
      }
      def initalizeContext(ctx: HttpContext, attachment: Any) {
        ctx.setAttribute(request_attachment, attachment)
      }
      def responseEntity(res: HttpResponse, ctx: HttpContext) =
        ctx.getAttribute(request_attachment) match {
          case callback: IOCallback => new CallbackEntity {
            def consumeContent(decoder: ContentDecoder, ioc: IOControl) {
              callback.with_decoder(res, decoder)
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
    ) {
      setEventListener(new org.apache.http.nio.protocol.EventListener {
        def connectionClosed(conn: NHttpConnection) { }
        def connectionOpen(conn: NHttpConnection) { }
        def connectionTimeout(conn: NHttpConnection) { }
        def fatalIOException(e: IOException, conn: NHttpConnection) {
          e.printStackTrace()
        }
        def fatalProtocolException(e: HttpException, conn: NHttpConnection) {
          e.printStackTrace()
        }
      })
    }


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
    val future = IOFuture(req, block)
    connect(host, credsopt, future)
    future
  }
  
  def executeWithCallback[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                             req: HttpRequest, callback: Callback) {
    connect(host, credsopt, IOCallback(req, callback))
  }

  private def connect[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                         attachment: RequestAttachment) {
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      io_reactor.connect(
        new InetSocketAddress(
          host.getHostName, 
          if (host.getPort == -1) 80 else host.getPort),
        null,
        attachment,
        null
      )
    }
  }

  type HttpPackage[T] = dispatch.futures.Futures.Future[T]

  def shutdown() {
    io_reactor.shutdown()
  }
}

trait CallbackEntity extends org.apache.http.nio.entity.ConsumingNHttpEntity  {
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
