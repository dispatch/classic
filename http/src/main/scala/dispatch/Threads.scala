package dispatch

import org.apache.http.{HttpHost,HttpRequest,HttpResponse}
import org.apache.http.client.methods.HttpRequestBase
import dispatch.futures._

/** Http with a thread-safe client and non-blocking interfaces */
trait ThreadSafety { self: BlockingHttp =>
  /** Maximum number of connections in pool, default is 50 */
  def maxConnections = 50
  /** Maximum number of connections one one route, default is maxConnections */
  def maxConnectionsPerRoute = maxConnections
  /** Shutdown connection manager if no longer in use. */

  override def make_client = new ThreadSafeHttpClient(maxConnections, maxConnectionsPerRoute)
  /** Shutdown connection manager, threads. */
  def shutdown() = client.getConnectionManager.shutdown()
}

trait ThreadFuture extends ThreadSafety { self: BlockingHttp =>
  type HttpPackage[T] = dispatch.futures.AbortableFuture[T]
  def pack[T](request: HttpRequestBase, result: => T) = new AbortableFuture[T] {
    val delegate = DefaultFuture.future(result)
    def apply() = delegate.apply()
    def isSet = delegate.isSet
    def abort() = request.abort()
  }
}

class FutureHttp extends BlockingHttp with ThreadFuture

/** Client with a ThreadSafeClientConnManager */
class ThreadSafeHttpClient(maxConnections: Int, maxConnectionsPerRoute: Int) 
    extends ConfiguredHttpClient {
  import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
  import org.apache.http.conn.ssl.SSLSocketFactory
  import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
  override def createClientConnectionManager = {
    val registry = new SchemeRegistry()
    registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()))
    registry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()))
    val cm = new ThreadSafeClientConnManager(registry)
    cm.setMaxTotal(maxConnections)
    cm.setDefaultMaxPerRoute(maxConnectionsPerRoute)
    cm
  }
}
