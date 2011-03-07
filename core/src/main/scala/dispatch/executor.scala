package dispatch

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.message.AbstractHttpMessage
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods._
import util.control.{Exception => Exc}

/** Defines request execution and response status code behaviors. Implemented methods are finalized
    as any overrides would be lost when instantiating delegate executors, is in Threads#future. 
    Delegates should chain to parent `pack` and `execute` implementations. */
trait HttpExecutor extends RequestLogging {
  /** Type of value returned from request execution */
  type HttpPackage[T]
  /** Execute the request against an HttpClient */
  def execute[T](host: HttpHost, creds: Option[Credentials], 
                 req: HttpRequestBase, block: HttpResponse => T): HttpPackage[T]

  def executeWithCallback[T](host: HttpHost, credsopt: Option[Credentials], 
                             req: HttpRequestBase, block: Callback[T]): HttpPackage[T]

  def catching[T](catcher: Exc.Catcher[T], block: => HttpPackage[T]): HttpPackage[T]

  /** Execute full request-response handler, response in package. */
  final def x[T](hand: Handler[T]): HttpPackage[T] = x(hand.request)(hand.block)
  /** Execute request with handler, response in package. */
  final def x[T](req: Request)(block: Handler.F[T]) = {
    val request = make_message(req)
    log.info("%s %s", req.host.getHostName, request.getRequestLine)
    req.headers.reverse.foreach {
      case (key, value) => request.addHeader(key, value)
    }
    execute(req.host, req.creds, request, { res =>
      val ent = res.getEntity match {
        case null => None
        case ent => Some(ent)
      }
      block(res.getStatusLine.getStatusCode, res, ent)
    })
  }
  /** Apply Response Handler if reponse code returns true from chk. */
  final def when[T](chk: Int => Boolean)(hand: Handler[T]) = x(hand.request) {
    case (code, res, ent) if chk(code) => hand.block(code, res, ent)
    case (code, _, Some(ent)) => 
      throw StatusCode(code, EntityUtils.toString(ent, hand.request.defaultCharset))
    case (code, _, _)         => 
      throw StatusCode(code, "[no entity]")
  }

  
  /** Apply handler block when response code is 200 - 204 */
  final def apply[T](hand: Handler[T]): HttpPackage[T] = catching(hand.catcher, { 
    (this when {code => (200 to 204) contains code})(hand)
  })


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
}

trait BlockingCallback { self: HttpExecutor =>
  def executeWithCallback[T](host: HttpHost, credsopt: Option[Credentials], 
                             req: HttpRequestBase, callback:  Callback[T]): HttpPackage[T] =
    catching(callback.catcher, execute(host, credsopt, req, { res =>
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
    }))
}

case class StatusCode(code: Int, contents:String)
  extends Exception("Unexpected response code: " + code + "\n" + contents)

/** Simple info logger */
trait Logger { def info(msg: String, items: Any*) }

trait RequestLogging {
  lazy val log: Logger = make_logger

  /** Info Logger for this instance, default returns Connfiggy if on classpath else console logger. */
  def make_logger = try {
    new Logger {
      def getObject(name: String) = Class.forName(name + "$").getField("MODULE$").get(null)
      // using delegate, repeating parameters aren't working with structural typing in 2.7.x
      val delegate = getObject("net.lag.logging.Logger")
        .asInstanceOf[{ def get(n: String): { def ifInfo(o: => Object) } }]
        .get(getClass.getCanonicalName)
      def info(msg: String, items: Any*) { delegate.ifInfo(msg.format(items: _*)) }
    }
  } catch {
    case _: ClassNotFoundException | _: NoClassDefFoundError => new Logger {
      def info(msg: String, items: Any*) { 
        println("INF: [console logger] dispatch: " + msg.format(items: _*)) 
      }
    }
  }
}
