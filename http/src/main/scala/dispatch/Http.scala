package dispatch

import collection.Map
import collection.immutable.{Map => IMap}
import java.net.URI

import org.apache.http.message.BasicHttpEntityEnclosingRequest
import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.client.methods._
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.auth.AuthScope
import org.apache.http.params.HttpProtocolParams

import org.apache.http.entity.{StringEntity,FileEntity}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.http.conn.params.ConnRouteParams
import org.apache.http.conn.ClientConnectionManager

import org.apache.commons.codec.binary.Base64.encodeBase64

case class StatusCode(code: Int, contents:String)
  extends Exception("Exceptional response code: " + code + "\n" + contents)

/** Simple info logger */
trait Logger { def info(msg: String, items: Any*) }

/** Http access point. Standard instances to be used by a single thread. */
class Http extends HttpExecutor {
  val client = make_client
  /** Defaults to dispatch.ConfiguredHttpClient, override to customize. */
  def make_client = new ConfiguredHttpClient
  
  lazy val log: Logger = make_logger

  /** Info Logger for this instance, default returns Connfiggy if on classpath else console logger. */
  def make_logger = try {
    new Logger {
      def getObject(name: String) = Class.forName(name + "$").getField("MODULE$").get(null)
      // using delegate, repeating parameters aren't working with structural typing in 2.7.x
      val delegate = getObject("net.lag.logging.Logger")
        .asInstanceOf[{ def get(n: String): { def ifInfo(o: => Object) } }]
        .get(classOf[Http].getCanonicalName)
      def info(msg: String, items: Any*) { delegate.ifInfo(msg.format(items: _*)) }
    }
  } catch {
    case _: ClassNotFoundException | _: NoClassDefFoundError => new Logger {
      def info(msg: String, items: Any*) { 
        println("INF: [console logger] dispatch: " + msg.format(items: _*)) 
      }
    }
  }
  
  /** Execute method for the given host, with logging. */
  private def execute(host: HttpHost, req: HttpRequest): HttpResponse = {
    log.info("%s %s", host.getHostName, req.getRequestLine)
    client.execute(host, req)
  }
  /** Execute for given optional parametrs, with logging. Creates local scope for credentials. */
  def execute[T](host: HttpHost, credsopt: Option[Credentials], 
                 req: HttpRequest, block: HttpResponse => T): HttpPackage[T] =
    block(
      credsopt.map { creds =>
        client.credentials.withValue(Some((
          new AuthScope(host.getHostName, host.getPort), creds)
        ))(execute(host, req))
      } getOrElse { execute(host, req) }
    )

  /** Unadorned handler return type */
  type HttpPackage[T] = T
  /** Synchronously access and return plain result value  */
  def pack[T](result: => T) = result
}

/** Defines request execution and response status code behaviors. Implemented methods are finalized
    as any overrides would be lost when instantiating delegate executors, is in Threads#future. 
    Delegates should chain to parent `pack` and `execute` implementations. */
trait HttpExecutor {
  /** Type of value returned from request execution */
  type HttpPackage[T]
  /** Execute the request against an HttpClient */
  def execute[T](host: HttpHost, creds: Option[Credentials], 
                 req: HttpRequest, block: HttpResponse => T): HttpPackage[T]
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
      finally { ent foreach (_.consumeContent) }
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
}

/** Used by client APIs to build Handler or other objects via chaining, completed implicitly.
  * @see Http#builder2product */
trait Builder[T] { def product:T }
object Builder {
  implicit def builderToProduct[T](builder: Builder[T]) = builder.product
}

/** May be used directly from any thread. */
object Http extends Http with Threads 
