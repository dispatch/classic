package dispatch.futures

import java.util.concurrent.{Executors,Callable,ExecutorService}

object Futures extends AvailableFutures {
  /** Structural type coinciding with scala.actors.Future */
  type Future[T] = Function0[T] {
    def isSet: Boolean
  }
}
object DefaultFuture extends JucFuture {
  lazy val futureExecutor = Executors.newCachedThreadPool
}
trait AvailableFutures {
  /** @return values of futures that have completed their processing */
  def available[T](fs: Iterable[Futures.Future[T]]) = fs filter { _.isSet } map { _() } toList
}
/** Interface to futures functionality */
trait Futures {
  def future[T](result: => T): Futures.Future[T]
}
/** A java.util.concurrent Future accessor, executor undefined */
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
/** Future accessor using a scala.actors future */
object ActorsFuture extends Futures {
  def future[T](result: => T) = scala.actors.Futures.future(result)
}
