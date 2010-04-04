package dispatch

import dispatch._
import dispatch.futures.Futures
import Http._

/** Module providing callback module behavior for dispatch Futures */
object Callbacks {
  import java.util.{ Timer, TimerTask }
  
  class HasCallbacks[T](f: Futures.Future[T]) {
    def afterMatch(cb: PartialFunction[Any, Unit]) = Callbacks.after(f)(cb)
    def after(cb: T => Any) = Callbacks.after(f)(cb)
  }
  
  implicit def f2hcb[T](f: Futures.Future[T]) = new HasCallbacks(f)
  
  /** execute a partial function after a given Futures.Future is set */
  def afterMatch[T](f: => Futures.Future[T])(cb: PartialFunction[Any, Unit]) =
    new Timer(true).scheduleAtFixedRate(new TimerTask() {
      def run = if(f.isSet) { cancel(); cb(f()) }
    }, 0, 200l)
  
  /** execute a fn that takes the result of a Futures.Future when set */
  def after[T](f: => Futures.Future[T])(cb: T => Any) =
    new Timer(true).scheduleAtFixedRate(new TimerTask() {
      def run = if(f.isSet) { cancel(); cb(f()) }
    }, 0, 200l)
}

/** Dispatch Futures callback mixin */
trait Callbacks[T] { this: Futures.Future[T] =>
  import java.util.{ Timer, TimerTask }
  val t = new Timer(true)
  
  def afterMatch[T](cb: PartialFunction[Any, Unit]) =
    t.scheduleAtFixedRate(new TimerTask() {
      def run = if(isSet) { cancel(); cb(asInstanceOf[Futures.Future[T]]()) }
    }, 0, 200l)
  
  def after[T](cb: T => Any) = 
    t.scheduleAtFixedRate(new TimerTask() {
      def run = if(isSet) { cancel(); cb(asInstanceOf[Futures.Future[T]]()) }
    }, 0, 200l)
}