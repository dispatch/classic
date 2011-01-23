package dispatch

import collection.Map
import collection.immutable.{Map => IMap}
import java.net.URI

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.client.methods._
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.auth.AuthScope
import org.apache.http.params.HttpProtocolParams

import org.apache.http.entity.{StringEntity,FileEntity}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.conn.params.ConnRouteParams
import org.apache.http.conn.ClientConnectionManager

import org.apache.commons.codec.binary.Base64.encodeBase64

/** Simple info logger */
trait Logger { def info(msg: String, items: Any*) }

/** Http access point. Standard instances to be used by a single thread. */
class Http extends HttpExecutor with BlockingCallback {
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

/** May be used directly from any thread. */
object Http extends Http with Threads 
