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
import org.apache.http.nio.client.HttpAsyncResponseConsumer
import org.apache.http.nio.concurrent.FutureCallback
import java.net.InetSocketAddress
import java.io.IOException

object Http {
  val socket_buffer_size = 8 * 1024
}

class Http extends dispatch.HttpExecutor {
  val client = new DefaultHttpAsyncClient()
  client.start()

  type HttpPackage[T] = dispatch.futures.AbortableFuture[T]

  def execute[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                 req: HttpRequestBase, block: HttpResponse => T) = {
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      var result: Option[T] = None
      val fut = client.execute(host, req, new FutureCallback[HttpResponse] {
        def cancelled() { }
        def completed(res: HttpResponse) { result = Some(block(res)) }
        def failed(ex: Exception) { ex.printStackTrace() }
      })
      new dispatch.futures.AbortableFuture[T] {
        def apply() = { fut.get(); result.get }
        def isSet = fut.isDone
        def abort() = req.abort()
      }
    }                     
  }
  
  def executeWithCallback[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                             req: HttpRequestBase, callback: Callback) {
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      val ioc = DecodingCallback(callback)
      var response: Option[HttpResponse] = None
      client.execute(new Producer(host, req), 
                     new HttpAsyncResponseConsumer[HttpResponse] {
        def cancel() { }
        def responseReceived(res: HttpResponse) {
          response = Some(res)
        }
        def consumeContent(decoder: ContentDecoder, ioctrl: IOControl) {
          ioc.with_decoder(response.get, decoder)
        }
        def failed(ex: Exception) { ex.printStackTrace() }
        def responseCompleted() {
          ioc.callback.finish(response.get)
        }
        def getResult() = response.get
      }, new FutureCallback[HttpResponse] {
        def cancelled() { }
        def completed(res: HttpResponse) { }
        def failed(ex: Exception) { ex.printStackTrace() }
      })
    }
  }

  def shutdown() {
    client.shutdown()
  }
}

case class DecodingCallback(callback: dispatch.Callback) {
  def with_decoder(response: HttpResponse, decoder: ContentDecoder) {
    val buffer = java.nio.ByteBuffer.allocate(Http.socket_buffer_size)
    val length = decoder.read(buffer)
    if (length > 0)
      callback.function(response, buffer.array(), length)
  }
}
