package dispatch

import org.apache.http.{HttpHost,HttpRequest,HttpResponse}

/** Http with a thread-safe client and non-blocking interfaces */
trait Threads extends Http {
  /** Maximum number of connections in pool, default is 50 */
  def maxConnections = 50
  /** Maximum number of connections one one route, default is maxConnections */
  def maxConnectionsPerRoute = maxConnections
  /** Shutdown connection manager if no longer in use. */

  override def make_client = new ThreadSafeHttpClient(maxConnections, maxConnectionsPerRoute)
  /** Shutdown connection manager, threads. (Needed to close console cleanly.) */
  def shutdown() = client.getConnectionManager.shutdown()
}

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
