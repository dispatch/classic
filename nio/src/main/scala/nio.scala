package dispatch.classic.nio

import dispatch.classic.{Callback,Request,ExceptionListener}
import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity,HttpException}
import org.apache.http.message.BasicHttpEntityEnclosingRequest
import org.apache.http.protocol._
import org.apache.http.impl.nio.client.{DefaultHttpAsyncClient,BasicHttpAsyncRequestProducer=>Producer}
import org.apache.http.client.methods._
import org.apache.http.params.CoreConnectionPNames
import org.apache.http.params.CoreProtocolPNames
import org.apache.http.nio.{ContentDecoder,IOControl,NHttpConnection}
import org.apache.http.nio.entity.{ConsumingNHttpEntity,BufferingNHttpEntity}
import org.apache.http.nio.client.HttpAsyncResponseConsumer
import org.apache.http.nio.concurrent.FutureCallback
import org.apache.http.nio.util.HeapByteBufferAllocator
import java.util.concurrent.Future
import java.io.IOException

object Http {
  val socket_buffer_size = 8 * 1024
}

class Http extends dispatch.classic.HttpExecutor {
  lazy val client = {
    val cl = make_client
    cl.start()
    cl
  }
  def make_client = new DefaultHttpAsyncClient

  type HttpPackage[T] = dispatch.classic.futures.StoppableFuture[T]

  abstract class StoppableConsumer[T](
    listener: ExceptionListener
  ) extends HttpAsyncResponseConsumer[T] {
    @volatile var stopping = false
    @volatile var exception: Option[Exception] = None
    private def setException(e: Exception) {
      exception = Some(e)
      listener.lift(e)
    }
    final override def consumeContent(decoder: ContentDecoder, ioctrl: IOControl) {
      try {
        if (stopping || exception.isDefined) {
          ioctrl.shutdown()
          cancel()
        }
        else consume(decoder, ioctrl)
      } catch {
        case e: Exception =>
          stopping = true
          setException(e)
      }
    }
    @volatile var response: Option[HttpResponse] = None
    def responseReceived(res: HttpResponse) {
      response = Some(res)
    }
    def consume(decoder: ContentDecoder, ioctrl: IOControl)
    def stop() { stopping = true }
    @volatile var result: Option[T] = None
    def responseCompleted() {
      try {
        result = Some(completeResult(response.getOrElse {
          sys.error("responseCompleted called but response unset")
        }))
      } catch {
        case e: Exception => setException(e)
      }
    }
    def completeResult(response: HttpResponse): T
    // asynchttpclient would a lot rather we return null here than throw an exception
    def getResult: T = result.getOrElse(null.asInstanceOf[T])
    def failed(ex: Exception) {
      setException(ex)
    }
  }
  class EmptyCallback[T] extends FutureCallback[T] {
    def cancelled() { }
    def completed(res: T) { }
    def failed(ex: Exception) { }
  }
  class ConsumerFuture[T](
    underlying: Future[T], 
    consumer: StoppableConsumer[T]
  ) extends dispatch.classic.futures.StoppableFuture[T] {
    def apply() = {
      val res = underlying.get()
      consumer.exception.foreach { throw _ }
      res
    }
    def isSet = consumer.exception.isDefined || underlying.isDone
    def stop() { 
      consumer.stop()
      underlying.cancel(true)
    }
  }
  /* substitute future used for blocking consumers */
  trait SubstituteFuture[T] extends dispatch.classic.futures.StoppableFuture[T] {
    def isSet = true
    def stop() {  }
  }
  class ExceptionFuture[T](e: Throwable) extends SubstituteFuture[T] {
    def apply() = throw e
  }

  def execute[T](host: HttpHost, 
                 credsopt: Option[dispatch.classic.Credentials], 
                 req: HttpRequestBase, 
                 block: HttpResponse => T,
                 listener: ExceptionListener) = {
    credsopt.map { creds =>
      sys.error("Not yet implemented, but you can force basic auth with as_!")
    } getOrElse {
      try {
        val consumer = new StoppableConsumer[T](listener) {
          @volatile var entity: Option[ConsumingNHttpEntity] = None
          def consume(decoder: ContentDecoder, ioctrl: IOControl) { synchronized {
            entity = entity.orElse {
              for {
                res <- response
                ent <- Option(res.getEntity)
              } yield (new BufferingNHttpEntity(ent, new HeapByteBufferAllocator))
            }
            entity.map { _.consumeContent(decoder, ioctrl) }
          } }
          def completeResult(res: HttpResponse) = {
            for (ent <- entity) {
              res.setEntity(ent)
              ent.finish()
            }
            block(res)
          }
          def cancel() {
            entity.map { _.finish() }
          }
        }
        new ConsumerFuture(
          client.execute(new Producer(host, req), consumer, new EmptyCallback[T]),
          consumer
        )
      } catch {
        case e => 
          listener.lift(e)
          new ExceptionFuture(e)
      }
    }
  }
  
  def executeWithCallback[T](host: HttpHost, credsopt: Option[dispatch.classic.Credentials], 
                             req: HttpRequestBase, callback: Callback[T]) = {
    credsopt.map { creds =>
      sys.error("Not yet implemented, but you can force basic auth with as_!")
    } getOrElse {
      val ioc = DecodingCallback(callback)
      val consumer = new StoppableConsumer[T](callback.listener) {
        override def responseReceived(res: HttpResponse) {
          response = Some(res)
        }
        def consume(decoder: ContentDecoder, ioctrl: IOControl) {
          ioc.with_decoder(response.get, decoder)
        }
        def completeResult(response: HttpResponse) = 
          callback.finish(response)
        def cancel() { }
      }
      new ConsumerFuture(
        client.execute(new Producer(host, req), consumer, new EmptyCallback[T]),
        consumer
      )
    }
  }
  /** Does nothing, NIO executor always consumes entities it creates */
  def consumeContent(entity: Option[HttpEntity]) { }
  def shutdownClient() {
    client.shutdown()
  }
}

case class DecodingCallback[T](callback: dispatch.classic.Callback[T]) {
  def with_decoder(response: HttpResponse, decoder: ContentDecoder) {
    val buffer = java.nio.ByteBuffer.allocate(Http.socket_buffer_size)
    val length = decoder.read(buffer)
    if (length > 0)
      callback.function(response, buffer.array(), length)
  }
}
