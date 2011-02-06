package dispatch.nio

import dispatch.{Callback,Request}
import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity,HttpException}
import org.apache.http.message.BasicHttpEntityEnclosingRequest
import org.apache.http.protocol._
import org.apache.http.impl.nio.client.{DefaultHttpAsyncClient,BasicHttpAsyncRequestProducer=>Producer}
import org.apache.http.client.methods._
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.nio.{ContentDecoder,IOControl,NHttpConnection}
import org.apache.http.nio.entity.ConsumingNHttpEntity
import org.apache.http.impl.nio.client.BasicHttpAsyncResponseConsumer
import org.apache.http.nio.concurrent.FutureCallback
import java.net.InetSocketAddress
import java.io.IOException

object Http {
  val socket_buffer_size = 8 * 1024
}

class Http extends dispatch.HttpExecutor {
  val client = new DefaultHttpAsyncClient
  client.start()

  type HttpPackage[T] = dispatch.futures.StoppableFuture[T]

  class StoppableConsumer extends BasicHttpAsyncResponseConsumer {
    @volatile private var stopped = false
    final override def consumeContent(decoder: ContentDecoder, ioctrl: IOControl) {
      if (stopped) {
        ioctrl.shutdown()
        cancel()
      }
      else consume(decoder, ioctrl)
    }
    def consume(decoder: ContentDecoder, ioctrl: IOControl) {
      super.consumeContent(decoder, ioctrl)
    }
    def stop() { stopped = true }
  }

  class ConsumerFuture(
    underlying: Future, 
    consumer: StoppableConsumer
  ) extends dispatch.futures.StoppableFuture[T] {
    def apply() = { fut.get(); result.get }
    def isSet = fut.isDone
    def stop() = { 
      consumer.stop()
      fut.cancel(true)
    }
  }

  def execute[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                 req: HttpRequestBase, block: HttpResponse => T) = {
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      var result: Option[T] = None
      val consumer = new StoppableConsumer
      val context = new BasicHttpContext
      val fut = client.execute(
        new Producer(host, req), 
        consumer,
        new FutureCallback[HttpResponse] {
          var isCancelled = false
          def cancelled() {
            isCancelled = true
          }
          def completed(res: HttpResponse) { 
            result = Some(block(res))
          }
          def failed(ex: Exception) { ex.printStackTrace() }
        }
      )
      new WrappedFuture
    }                     
  }
  
  def executeWithCallback[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                             req: HttpRequestBase, callback: Callback[T]) = {
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      val ioc = DecodingCallback(callback)
      var result: Option[T] = None
      val consumer = new StoppableConsumer {
        override def responseReceived(res: HttpResponse) {
          response = Some(res)
        }
        override def consume(decoder: ContentDecoder, ioctrl: IOControl) {
          ioc.with_decoder(response.get, decoder)
        }
      }

      val fut = client.execute(new Producer(host, req), 
                               consumer,
                               new FutureCallback[HttpResponse] {
        def cancelled() { }
        def completed(res: HttpResponse) {
          result = Some(ioc.callback.finish(response))
        }
        def failed(ex: Exception) { ex.printStackTrace() }
      })
      new dispatch.futures.StoppableFuture[T] {
        def apply() = { fut.get(); result.get }
        def isSet = fut.isDone
        def stop() = { consumer.stop(); fut.cancel(true) }
      }
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
