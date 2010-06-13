package dispatch

/** Http with a thread-safe client and non-blocking interfaces */
trait Threads extends Http with FuturableExecutor {
  override val client = new ThreadSafeHttpClient(maxConnections, maxConnectionsPerRoute)
  /** Maximum number of connections in pool, default is 50 */
  def maxConnections = 50
  /** Maximum number of connections one one route, default is maxConnections */
  def maxConnectionsPerRoute = maxConnections
  /** Shutdown connection manager if no longer in use. */
  def shutdown() = client.getConnectionManager.shutdown()
}
trait FuturableExecutor extends HttpExecutor {
  import dispatch.futures.Futures
  /** @return an executor that will call `error` on any exception */
  def on_error (error: PartialFunction[Throwable, Unit]) = new FuturableExecutor {
    val execute = FuturableExecutor.this.execute
    type HttpPackage[T] = FuturableExecutor.this.HttpPackage[T]
    def pack[T](result: => T) = try { FuturableExecutor.this.pack(result) } catch {
      case e if error.isDefinedAt(e) => error(e); throw e
    }
    override def http_future = FuturableExecutor.this.http_future
  }
  /** @return an asynchronous Http interface that packs responses through a Threads#Future */
  lazy val future = new HttpExecutor {
    val execute = FuturableExecutor.this.execute
    type HttpPackage[T] = Futures.Future[FuturableExecutor.this.HttpPackage[T]]
    def pack[T](result: => T) = http_future.future(FuturableExecutor.this.pack(result))
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
    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
    registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
    val cm = new ThreadSafeClientConnManager(getParams(), registry)
    cm.setMaxTotalConnections(maxConnections)
    cm.setDefaultMaxPerRoute(maxConnectionsPerRoute)
    cm
  }
}
