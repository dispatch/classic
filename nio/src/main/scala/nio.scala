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

  def execute[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                 req: HttpRequest, block: HttpResponse => T): HttpPackage[T] = {
    credsopt.map { creds =>
      error("todo")
    } getOrElse {
      var result: Option[T] = None
      val fut = client.execute(host, req, new FutureCallback[HttpResponse] {
        def cancelled() { }
        def completed(res: HttpResponse) { result = Some(block(res)) }
        def failed(ex: Exception) { ex.printStackTrace() }
      })
      new (() => T) {
        def apply() = { fut.get(); result.get }
        def isSet = fut.isDone
      }
    }                     
  }
  
  def make_message(req: Request) = {
    req.method.toUpperCase match {
      case HttpGet.METHOD_NAME => new HttpGet(req.path)
      case HttpHead.METHOD_NAME => new HttpHead(req.path)
      case HttpDelete.METHOD_NAME => new HttpDelete(req.path)
      case method => 
        val message = method match {
          case HttpPost.METHOD_NAME => new HttpPost(req.path)
          case HttpPut.METHOD_NAME => new HttpPut(req.path)
        }
        req.body.foreach(message.setEntity)
        message
    }
  }
  def executeWithCallback[T](host: HttpHost, credsopt: Option[dispatch.Credentials], 
                             req: HttpRequest, callback: Callback) {
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

  type HttpPackage[T] = dispatch.futures.Futures.Future[T]

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
