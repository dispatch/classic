package dispatch.nio

import dispatch.{Callback,Request}
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

class Http extends dispatch.HttpExecutor {
  lazy val client = {
    val cl = make_client
    cl.start()
    cl
  }
  def make_client = new DefaultHttpAsyncClient

  type HttpPackage[T] = dispatch.futures.StoppableFuture[T]

  trait StoppableConsumer[T] extends HttpAsyncResponseConsumer[T] {
    @volatile var stopping = false
    final override def consumeContent(decoder: ContentDecoder, ioctrl: IOControl) {
      if (stopping) {
        ioctrl.shutdown()
        cancel()
      }
      else consume(decoder, ioctrl)
    }
    @volatile var response: Option[HttpResponse] = None
    def responseReceived(res: HttpResponse) {
      response = Some(res)
    }
    def consume(decoder: ContentDecoder, ioctrl: IOControl)
    def stop() { stopping = true }
    @volatile var result: Option[T] = None
    def responseCompleted() {
      result = Some(completeResult(response.getOrElse {
        error("responseCompleted called but response unset")
      }))
    }
    def completeResult(response: HttpResponse): T
    def getResult: T = result.getOrElse {
      error("getResult called but result is unset")
    }
    def failed(ex: Exception) {
      println(ex)
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
  ) extends dispatch.futures.StoppableFuture[T] {
    def apply() = underlying.get()
    def isSet = underlying.isDone
    def stop() = { 
      consumer.stop()
      underlying.cancel(true)
    }
  }

  def execute[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                 req: HttpRequestBase, block: HttpResponse => T) = {
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      val consumer = new StoppableConsumer[T] {
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
    }
  }
  
  def executeWithCallback[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                             req: HttpRequestBase, callback: Callback[T]) = {
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      val ioc = DecodingCallback(callback)
      val consumer = new StoppableConsumer[T] {
        override def responseReceived(res: HttpResponse) {
          response = Some(res)
        }
        override def consume(decoder: ContentDecoder, ioctrl: IOControl) {
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

  def shutdown() {
    client.shutdown()
  }
}

case class DecodingCallback[T](callback: dispatch.Callback[T]) {
  def with_decoder(response: HttpResponse, decoder: ContentDecoder) {
    val buffer = java.nio.ByteBuffer.allocate(Http.socket_buffer_size)
    val length = decoder.read(buffer)
    if (length > 0)
      callback.function(response, buffer.array(), length)
  }
}
