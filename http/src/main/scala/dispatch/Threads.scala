package dispatch

import java.util.concurrent.{Executors,Callable,ExecutorService}

/** Http with a thread-safe client and non-blocking interfaces */
trait Threads extends Http with FuturableExecutor {
  override val client = new ThreadSafeHttpClient
  /** Shutdown connection manager, threads. (Needed to close console cleanly.) */
  def shutdown() = client.getConnectionManager.shutdown()
}
trait FuturableExecutor extends HttpExecutor {
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
    type HttpPackage[T] = Futures#Future[FuturableExecutor.this.HttpPackage[T]]
    def pack[T](result: => T) = http_future.future(FuturableExecutor.this.pack(result))
  }
  def http_future: Futures = DefaultJucFuture
}
/** Accessors for futures used by FuturableExecutor */
trait Futures {
  /** Structural type coinciding with scala.actors.Future */
  type Future[T] = Function0[T] {
    def isSet: Boolean
  }
  def future[T](result: => T): Future[T]
}
/** A java.util.concurrent Future accessor */
trait JucFuture extends Futures {
  def future[T](result: => T) = new JucFuture(result)
  /** Implement to customize the java.util.concurrent Executor, defaults to Executors.newCachedThreadPool */
  val futureExecutor: ExecutorService
  /** Wraps java.util.concurrent.Future */
  class JucFuture[T](f: => T) extends Function0[T] {
    val delegate = futureExecutor.submit(new Callable[T]{
      def call = f
    })
    def isSet = delegate.isDone
    def apply() = delegate.get()
  }
}
/** Future accessor using a cached therad pool */
object DefaultJucFuture extends JucFuture {
   lazy val futureExecutor = Executors.newCachedThreadPool
}
/** Future accessor using a scala.actors future */
object ActorsFuture extends Futures {
  def future[T](result: => T) = scala.actors.Futures.future(result)
}

/** Client with a ThreadSafeClientConnManager */
class ThreadSafeHttpClient extends ConfiguredHttpClient {
  import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
  import org.apache.http.conn.ssl.SSLSocketFactory
  import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
  override def createClientConnectionManager() = {
    val registry = new SchemeRegistry()
    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
    registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
    new ThreadSafeClientConnManager(getParams(), registry)
  }
}