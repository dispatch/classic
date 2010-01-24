package dispatch

import java.util.concurrent.{Executors,Callable}

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
    override def futureExecutor = FuturableExecutor.this.executor
  }
  /** Structural type coinciding with scala.actors.Future */
  type Future[T] = Function0[T] {
    def isSet: Boolean
  }
  /** Override to customize the java.util.concurrent Executor, defaults to Executors.newCachedThreadPool */
  def futureExecutor = Executors.newCachedThreadPool
  private lazy val executor = futureExecutor
  /** Wraps java.util.concurrent.Future */
  class ConcFuture[T](f: => T) extends Function0[T] {
    val delegate = executor.submit(new Callable[T]{
      def call = f
    })
    def isSet = delegate.isDone
    def apply() = delegate.get()
  }
  /** @return an asynchronous Http interface that packs responses through a Threads#Future */
  lazy val future = concFuture
  /** @return interface using concurrent future */
  def concFuture = new FutureExecutor {
    def future[T](result: => T) = new ConcFuture(result)
  }
  /** @return interface using actors future */
  def actorsFuture = new FutureExecutor {
    def future[T](result: => T) = scala.actors.Futures.future(result)
  }
  /** Base trait for excecutors that return a Future */
  trait FutureExecutor extends HttpExecutor {
    def future[T](result: => T): Future[T]
    val execute = FuturableExecutor.this.execute
    type HttpPackage[T] = Future[FuturableExecutor.this.HttpPackage[T]]
    def pack[T](result: => T) = future(FuturableExecutor.this.pack(result))
  }
}