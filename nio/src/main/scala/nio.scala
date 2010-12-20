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
  
  val execute: (HttpHost, Option[Credentials], HttpRequest) => HttpResponse = {
    case (host, Some(creds), req) => error("todo")
    case (host, _, req) =>
      val params = new org.apache.http.params.SyncBasicHttpParams
      params
          .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
          .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
          .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
          .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
          .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)

      val handler = new org.apache.http.nio.protocol.AsyncNHttpClientHandler(
        new ImmutableHttpProcessor(Array(
          new RequestContent(),
          new RequestTargetHost(),
          new RequestConnControl(),
          new RequestUserAgent(),
          new RequestExpectContinue())),
        new org.apache.http.nio.protocol.NHttpRequestExecutionHandler {
          def submitRequest(ctx: HttpContext) = {
            val flag = "umm_request_already_submitted"
            if (ctx.getAttribute(flag) == null) {
              ctx.setAttribute(flag, true)
              println("submitting request")
              req
            } else null
          }
          def finalizeContext(ctx: HttpContext) { }
          def handleResponse(response: HttpResponse, ctx: HttpContext) { }
          def initalizeContext(ctx: HttpContext, attachment: Any) { }
          def responseEntity(res: HttpResponse, ctx: HttpContext) =
            new org.apache.http.nio.entity.BufferingNHttpEntity(
              res.getEntity, new org.apache.http.nio.util.DirectByteBufferAllocator)
        },
        new org.apache.http.impl.DefaultConnectionReuseStrategy,
        params
      )
      val ioReactor = new DefaultConnectingIOReactor(2, params)
      (new Thread(new Runnable {
        def run { ioReactor.execute(new DefaultClientIOEventDispatch(handler, params)) }
      })).start()
      ioReactor.connect(
        new InetSocketAddress(
          host.getHostName, 
          if (host.getPort == -1) 80 else host.getPort),
        null,
        req,
        null
      )
      Thread.sleep(3000)
      ioReactor.shutdown()
      error("help")
  }
  /** Unadorned handler return type */
  type HttpPackage[T] = T
  def pack[T](result: => T) = result
}
