package dispatch

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.message.AbstractHttpMessage
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods._

/** Defines request execution and response status code behaviors. Implemented methods are finalized
    as any overrides would be lost when instantiating delegate executors, is in Threads#future. 
    Delegates should chain to parent `pack` and `execute` implementations. */
trait HttpExecutor extends RequestLogging {
  /** Type of value returned from request execution */
  type HttpPackage[T]
  /** Execute the request against an HttpClient */
  def execute[T](host: HttpHost, 
                 creds: Option[Credentials], 
                 req: HttpRequestBase, 
                 block: HttpResponse => T,
                 listener: ExceptionListener): HttpPackage[T]

  def executeWithCallback[T](host: HttpHost, credsopt: Option[Credentials], 
                             req: HttpRequestBase, block: Callback[T]): HttpPackage[T]

  @deprecated("Use x[T](hand: Handler[T]) instead. Construct a Handler if needed.")
  final def x[T](req: Request)(block: Handler.F[T]): HttpPackage[T] = {
    x(Handler(req, block))
  }
  /** Execute full request-response handler, response in package. */
  final def x[T](hand: Handler[T]): HttpPackage[T] = {
    val req = hand.request
    val request = make_message(hand.request)
    log.info("%s %s", req.host.getHostName, request.getRequestLine)
    req.headers.reverse.foreach {
      case (key, value) => request.addHeader(key, value)
    }
    execute(req.host, req.creds, request, { res =>
      val ent = res.getEntity match {
        case null => None
        case ent => Some(ent)
      }
      val result = hand.block(res.getStatusLine.getStatusCode, res, ent)
      // only handlers that use the content stream have closed it
      ent.foreach(EntityUtils.consume)
      result
    }, hand.listener)
  }
  /** Apply Response Handler if reponse code returns true from chk. */
  final def when[T](chk: Int => Boolean)(hand: Handler[T]) = 
    x(hand.copy(block= {
      case (code, res, ent) if chk(code) => hand.block(code, res, ent)
      case (code, _, Some(ent)) => 
        throw StatusCode(code, EntityUtils.toString(ent, hand.request.defaultCharset))
      case (code, _, _)         => 
        throw StatusCode(code, "[no entity]")
    }))
  
  /** Apply handler block when response code is 200 - 204 */
  final def apply[T](hand: Handler[T]): HttpPackage[T] = 
    (this when {code => (200 to 204) contains code})(hand)

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
    log.info("%s %s", req.host.getHostName, request.getRequestLine)
    req.headers.reverse.foreach {
      case (key, value) => request.addHeader(key, value)
    }
    executeWithCallback(req.host, req.creds, request, callback)
  }
  private var isShutdown = false
  /** Release resources held by the executor. */
  def shutdown() {
    shutdownClient();
    isShutdown = true
  }
  protected def shutdownClient(): Unit
  /** Call shutdown if not already shutdown, issue warning */
  override def finalize() {
    if (!isShutdown) {
      log.warn(
        "Shutting down garbage-collected HttpExecutor--" +
        "Call shutdown() explicitly to avoid resource leaks!"
      )
      shutdown()
    }
    super.finalize()
  }
}

trait BlockingCallback { self: HttpExecutor =>
  def executeWithCallback[T](host: HttpHost, 
                             credsopt: Option[Credentials], 
                             req: HttpRequestBase, 
                             callback:  Callback[T]): HttpPackage[T] =
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
    }, callback.listener)
}

case class StatusCode(code: Int, contents:String)
  extends Exception("Unexpected response code: " + code + "\n" + contents)

/** Simple info and warn logger */
trait Logger {
  def info(msg: String, items: Any*)
  def warn(msg: String, items: Any*)
}

trait RequestLogging {
  lazy val log: Logger = make_logger

  /** Logger for this executor, logs to console. */
  def make_logger =
    new Logger {
      def info(msg: String, items: Any*) { 
        println("INF: [console logger] dispatch: " + 
                msg.format(items: _*))
      }
      def warn(msg: String, items: Any*) { 
        println("WARN: [console logger] dispatch: " +
                msg.format(items: _*))
      }
    }
}
