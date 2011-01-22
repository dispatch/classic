package dispatch

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.message.BasicHttpEntityEnclosingRequest
import org.apache.http.util.EntityUtils

/** Defines request execution and response status code behaviors. Implemented methods are finalized
    as any overrides would be lost when instantiating delegate executors, is in Threads#future. 
    Delegates should chain to parent `pack` and `execute` implementations. */
trait HttpExecutor {
  /** Type of value returned from request execution */
  type HttpPackage[T]
  /** Execute the request against an HttpClient */
  def execute[T](host: HttpHost, creds: Option[Credentials], 
                 req: HttpRequest, block: HttpResponse => T): HttpPackage[T]

  def executeWithCallback[T](host: HttpHost, credsopt: Option[Credentials], 
                             req: HttpRequest, block:  Callback.Function)
  /** Execute full request-response handler, response in package. */
  final def x[T](hand: Handler[T]): HttpPackage[T] = x(hand.request)(hand.block)
  /** Execute request with handler, response in package. */
  final def x[T](req: Request)(block: Handler.F[T]) = {
    val request = new BasicHttpEntityEnclosingRequest(req.method, req.path)
    req.headers.reverse.foreach {
      case (key, value) => request.addHeader(key, value)
    }
    req.body.foreach(request.setEntity)
    execute(req.host, req.creds, request, { res =>
      val ent = res.getEntity match {
        case null => None
        case ent => Some(ent)
      }
      try { block(res.getStatusLine.getStatusCode, res, ent) }
      finally { ent foreach EntityUtils.consume }
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

  /** Apply handler block when response code is 200 - 204 */
  final def apply[T](callback: Callback) = {
    val req = callback.request
    val request = new BasicHttpEntityEnclosingRequest(req.method, req.path)
    req.headers.reverse.foreach {
      case (key, value) => request.addHeader(key, value)
    }
    req.body.foreach(request.setEntity)
    executeWithCallback(req.host, req.creds, request, callback.function)
  }
}

case class StatusCode(code: Int, contents:String)
  extends Exception("Exceptional response code: " + code + "\n" + contents)
