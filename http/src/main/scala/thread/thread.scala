package dispatch.thread

import org.apache.http.{HttpHost,HttpRequest,HttpResponse}
import org.apache.http.client.methods.HttpRequestBase
import dispatch._
import dispatch.futures._

/** Http with a thread-safe client */
trait Safety { self: BlockingHttp =>
  /** Maximum number of connections in pool, default is 50 */
  def maxConnections = 50
  /** Maximum number of connections one one route, default is maxConnections */
  def maxConnectionsPerRoute = maxConnections
  /** Shutdown connection manager if no longer in use. */

  override def make_client = new ThreadSafeHttpClient(
    credentials, maxConnections, maxConnectionsPerRoute)
}

/** Wraps each call in a (threaded) future.  */
trait Future extends Safety { self: BlockingHttp =>
  type HttpPackage[T] = StoppableFuture[T]
  def pack[T](request: { def abort() }, result: => T) = new StoppableFuture[T] {
    val delegate = DefaultFuture.future(result)
    def apply() = delegate.apply()
    def isSet = delegate.isSet
    def stop() = request.abort()
  }
}

class Http extends BlockingHttp with Future

/** Client with a ThreadSafeClientConnManager */
class ThreadSafeHttpClient(
  credentials: Http.CurrentCredentials,
  maxConnections: Int, 
  maxConnectionsPerRoute: Int
) extends ConfiguredHttpClient(credentials) {
  import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
  import org.apache.http.conn.ssl.SSLSocketFactory
  import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
  override def createClientConnectionManager = {
    val cm = new ThreadSafeClientConnManager()
    cm.setMaxTotal(maxConnections)
    cm.setDefaultMaxPerRoute(maxConnectionsPerRoute)
    cm
  }
}
