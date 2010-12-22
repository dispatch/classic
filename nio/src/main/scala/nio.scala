package dispatch.nio

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.auth.{AuthScope,UsernamePasswordCredentials,Credentials}
import org.apache.http.client.methods._
import org.apache.http.protocol._
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import java.net.InetSocketAddress

class HttpNio extends dispatch.HttpExecutor {
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
        val flag = "umm_request_already_submitted"
        if (ctx.getAttribute(flag) == null) {
          ctx.setAttribute(flag, true)
          println("submitting request")
          ctx.getAttribute(request_response) match {
            case (req: HttpRequest, _) => req
            case _ => error("request_response of wrong type")
          }
        } else null
      }
      def finalizeContext(ctx: HttpContext) { }
      def handleResponse(response: HttpResponse, ctx: HttpContext) {
        ctx.getAttribute(request_response) match {
          case (_, block: (HttpResponse => Any)) => block(response)
          case _ => error("request_response of wrong type")
        }
      }
      def initalizeContext(ctx: HttpContext, attachment: Any) {
        ctx.setAttribute(request_response, attachment)
      }
      def responseEntity(res: HttpResponse, ctx: HttpContext) =
        new org.apache.http.nio.entity.BufferingNHttpEntity(
          res.getEntity, new org.apache.http.nio.util.DirectByteBufferAllocator)
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
  
  def execute[T](host: HttpHost, credsopt: Option[Credentials], 
                 req: HttpRequest, block: HttpResponse => T): HttpPackage[T] =
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      io_reactor.connect(
        new InetSocketAddress(
          host.getHostName, 
          if (host.getPort == -1) 80 else host.getPort),
        null,
        (req, block),
        null
      )
      Thread.sleep(3000)
      io_reactor.shutdown()
      error("help")
    }
  /** Unadorned handler return type */
  type HttpPackage[T] = T
  def pack[T](result: => T) = result
}
