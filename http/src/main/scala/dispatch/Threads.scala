package dispatch

import org.apache.http.{HttpHost,HttpRequest,HttpResponse}

/** Http with a thread-safe client and non-blocking interfaces */
trait Threads extends Http with FuturableExecutor {
  /** Maximum number of connections in pool, default is 50 */
  def maxConnections = 50
  /** Maximum number of connections one one route, default is maxConnections */
  def maxConnectionsPerRoute = maxConnections
  /** Shutdown connection manager if no longer in use. */

  override def make_client = new ThreadSafeHttpClient(maxConnections, maxConnectionsPerRoute)
  /** Shutdown connection manager, threads. (Needed to close console cleanly.) */
  def shutdown() = client.getConnectionManager.shutdown()
}
trait FuturableExecutor extends HttpExecutor {
  import dispatch.futures.Futures
  /** @return an executor that will call `error` on any exception */
  def on_error (error: PartialFunction[Throwable, Unit]) = new FuturableExecutor {
    def execute[T](host: HttpHost, creds: Option[Credentials], 
                   req: HttpRequest, block: HttpResponse => T) =
      try { 
        FuturableExecutor.this.execute(host, creds, req, block)
      } catch {
        case e if error.isDefinedAt(e) => error(e); throw e
      }
    type HttpPackage[T] = FuturableExecutor.this.HttpPackage[T]
    override def http_future = FuturableExecutor.this.http_future
  }
  /** @return an asynchronous Http interface that packs responses through a Threads#Future */
  lazy val future = new HttpExecutor {
    def execute[T](host: HttpHost, creds: Option[Credentials], 
                   req: HttpRequest, block: HttpResponse => T) =
       http_future.future(FuturableExecutor.this.execute(host, creds, req, block))
    type HttpPackage[T] = Futures.Future[FuturableExecutor.this.HttpPackage[T]]
  }
  /** Override to use any Futures implementation */
  def http_future: Futures = dispatch.futures.DefaultFuture
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
