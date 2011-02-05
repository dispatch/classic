package dispatch

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.message.AbstractHttpMessage
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods._

/** Defines request execution and response status code behaviors. Implemented methods are finalized
    as any overrides would be lost when instantiating delegate executors, is in Threads#future. 
    Delegates should chain to parent `pack` and `execute` implementations. */
trait HttpExecutor {
  /** Type of value returned from request execution */
  type HttpPackage[T]
  /** Execute the request against an HttpClient */
  def execute[T](host: HttpHost, creds: Option[Credentials], 
                 req: HttpRequestBase, block: HttpResponse => T): HttpPackage[T]

  def executeWithCallback[T](host: HttpHost, credsopt: Option[Credentials], 
                             req: HttpRequestBase, block: Callback[T]): HttpPackage[T]
  /** Execute full request-response handler, response in package. */
  final def x[T](hand: Handler[T]): HttpPackage[T] = x(hand.request)(hand.block)
  /** Execute request with handler, response in package. */
  final def x[T](req: Request)(block: Handler.F[T]) = {
    val request = make_message(req)
    req.headers.reverse.foreach {
      case (key, value) => request.addHeader(key, value)
    }
    execute(req.host, req.creds, request, { res =>
      val ent = res.getEntity match {
        case null => None
        case ent => Some(ent)
      }
      try { block(res.getStatusLine.getStatusCode, res, ent) }
    })
  }
  /** Apply Response Handler if reponse code returns true from chk. */
  final def when[T](chk: Int => Boolean)(hand: Handler[T]) = x(hand.request) {
    case (code, res, ent) if chk(code) => hand.block(code, res, ent)
    case (code, _, Some(ent)) => throw StatusCode(code, EntityUtils.toString(ent, Request.factoryCharset))
    case (code, _, _)         => throw StatusCode(code, "[no entity]")
  }
  
  /** Apply handler block when response code is 200 - 204 */
  final def apply[T](hand: Handler[T]) = (this when {code => (200 to 204) contains code})(hand)

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

  /** Apply handler block when response code is 200 - 204 */
  final def apply[T](callback: Callback[T]) = {
    val req = callback.request
    val request = make_message(req)
    req.headers.reverse.foreach {
      case (key, value) => request.addHeader(key, value)
    }
    executeWithCallback(req.host, req.creds, request, callback)
  }
}

trait BlockingCallback { self: HttpExecutor =>
  def executeWithCallback[T](host: HttpHost, credsopt: Option[Credentials], 
                             req: HttpRequestBase, callback:  Callback[T]) = {
    execute(host, credsopt, req, { res =>
      res.getEntity match {
        case null => callback.finish(res)
        case entity =>
          val stm = entity.getContent
          val buf = new Array[Byte](8 * 1024)
          var count = 0
          while ({count = stm.read(buf); count} > -1)
            callback.function(res, buf, count)
          stm.close()
          callback.finish(res)
      }
    })
  }
}


case class StatusCode(code: Int, contents:String)
  extends Exception("Exceptional response code: " + code + "\n" + contents)
