package dispatch.classic.gae

import java.net._
import java.util.concurrent.TimeUnit
import org.apache.http.conn._
import org.apache.http.params._
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.conn.scheme._

object GAEConnectionManager extends GAEConnectionManager

// adapted from this Java impl: http://esxx.blogspot.com/2009/06/using-apaches-httpclient-on-google-app.html

class GAEConnectionManager extends ClientConnectionManager {
  val no_socket_factory = new SchemeSocketFactory {
    def connectSocket(sock: Socket, remote:InetSocketAddress, local: InetSocketAddress, params: HttpParams): Socket = null
    def createSocket(params: HttpParams): Socket = null
    def isSecure(s: Socket): Boolean = false
  }

  protected val schemeRegistry = new SchemeRegistry
  schemeRegistry.register(new Scheme("http",  80, no_socket_factory))
  schemeRegistry.register(new Scheme("https", 443, no_socket_factory))

  override def getSchemeRegistry: SchemeRegistry = schemeRegistry

  override def requestConnection(route: HttpRoute, state: AnyRef): ClientConnectionRequest = {
    val mgr = this
    new ClientConnectionRequest {
      def abortRequest = {}
      def getConnection(timeout: Long, tunit: TimeUnit): ManagedClientConnection = {
        new GAEClientConnection(mgr, route, state)
      }
    }
  }

  override def releaseConnection(conn: ManagedClientConnection, validDuration: Long, timeUnit: TimeUnit) = {}
  override def closeIdleConnections(idletime: Long, tunit: TimeUnit) = {}
  override def closeExpiredConnections = {}
  override def shutdown = {}
}

import java.io._
import java.net._
import java.util.concurrent.TimeUnit
import org.apache.http._
import org.apache.http.conn._
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.message.BasicHttpResponse
import org.apache.http.params._
import org.apache.http.protocol._

import com.google.appengine.api.urlfetch._

object GAEClientConnection {
  private var urlFS: URLFetchService = URLFetchServiceFactory.getURLFetchService
}

class GAEClientConnection(var cm: ClientConnectionManager, var route: HttpRoute, var state: AnyRef, var closed: Boolean) extends ManagedClientConnection {
  import GAEClientConnection._

  def this(cm: ClientConnectionManager, route: HttpRoute, state: AnyRef) =
    this(cm, route, state, true)

  private var request: HTTPRequest = _
  private var response: HTTPResponse = _
  private var reusable: Boolean = _

  override def isSecure: Boolean = route.isSecure
  override def getRoute: HttpRoute = route
  override def getSSLSession: javax.net.ssl.SSLSession = null

  override def open(route: HttpRoute, context: HttpContext, params: HttpParams) = {
    close
    this.route = route
  }

  override def tunnelTarget(secure: Boolean, params: HttpParams) = throw new IOException("tunnelTarget not supported")
  override def tunnelProxy(next: HttpHost, secure: Boolean, params: HttpParams) = throw new IOException("tunnelProxy not supported")
  override def layerProtocol(context: HttpContext, params: HttpParams) = throw new IOException("layerProtocol not supported")
  override def markReusable = reusable = true
  override def unmarkReusable = reusable = false
  override def isMarkedReusable: Boolean = reusable
  override def setState(state: AnyRef) = this.state = state
  override def getState: AnyRef = state
  override def setIdleDuration(duration: Long, unit: TimeUnit) = {}
  override def isResponseAvailable(timeout: Int): Boolean = response != null

  override def sendRequestHeader(request: HttpRequest) = {
    try {
      val host = route.getTargetHost
      val port = if (host.getPort() == -1) "" else ":" + host.getPort()
      val uri = new URI(host.getSchemeName
                        + "://"
                        + host.getHostName
                        + port
                        + request.getRequestLine.getUri)

      this.request = new HTTPRequest(uri.toURL,
                                     HTTPMethod.valueOf(request.getRequestLine.getMethod),
                                     FetchOptions.Builder.disallowTruncate.doNotFollowRedirects)
    }
    catch {
      case ex: URISyntaxException =>
        throw new IOException("Malformed request URI: " + ex.getMessage)
      case ex: IllegalArgumentException =>
        throw new IOException("Unsupported HTTP method: " + ex.getMessage)
    }

    for (h <- request.getAllHeaders)
      this.request.addHeader(new HTTPHeader(h.getName, h.getValue))
  }

  override def sendRequestEntity(request: HttpEntityEnclosingRequest ) = {
    val baos = new ByteArrayOutputStream
    if (request.getEntity != null)
      request.getEntity.writeTo(baos)
    this.request.setPayload(baos.toByteArray)
  }

  override def receiveResponseHeader: HttpResponse = {
    if (this.response == null) {
      flush
    }

    val response: HttpResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1),
                                                       this.response.getResponseCode,
                                                       null)

    val i = this.response.getHeaders.iterator
    while (i.hasNext) {
      val h = i.next
      response.addHeader(h.getName, h.getValue)
    }

    response
  }

  override def receiveResponseEntity(response: HttpResponse) = {
    if (this.response == null)
      throw new IOException("receiveResponseEntity called on closed connection")

    val bae = new ByteArrayEntity(this.response.getContent)
    bae.setContentType(response.getFirstHeader("Content-Type"))
    response.setEntity(bae)

    //response = null
  }

  override def flush = {
    if (request != null) {
      response = urlFS.fetch(request)
      request = null
    }
    else {
      response = null
    }
  }

  override def close {
    request  = null
    response = null
    closed   = true
  }

  override def isOpen: Boolean = (request != null) || (response != null)
  override def isStale: Boolean = !isOpen && !closed

  override def setSocketTimeout(timeout: Int) = {}
  override def getSocketTimeout: Int = -1

  override def shutdown = close
  override def getMetrics: HttpConnectionMetrics = null

  override def getLocalAddress: InetAddress = null
  override def getLocalPort: Int = 0
  override def getRemoteAddress: InetAddress = null
  override def getRemotePort: Int = {
    val host: HttpHost = route.getTargetHost
    cm.getSchemeRegistry.getScheme(host).resolvePort(host.getPort)
  }

  override def releaseConnection = cm.releaseConnection(this, java.lang.Long.MAX_VALUE, TimeUnit.MILLISECONDS)

  override def abortConnection = {
    unmarkReusable
    shutdown
  }
}
