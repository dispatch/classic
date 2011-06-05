package dispatch

import collection.Map
import collection.immutable.{Map => IMap}
import java.net.URI

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.auth.AuthScope
import org.apache.http.params.HttpProtocolParams

import org.apache.http.entity.{StringEntity,FileEntity}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.conn.params.ConnRouteParams
import org.apache.http.conn.ClientConnectionManager

/** Http access point. Standard instances to be used by a single thread. */
class Http extends BlockingHttp {
  /** Unadorned handler return type */
  type HttpPackage[T] = T
  /** Synchronously access and return plain result value  */
  def pack[T](req: { def abort() }, result: => T) = result
}

/** May be used directly from any thread. */
object Http extends Http with thread.Safety  {
  type CurrentCredentials = util.DynamicVariable[Option[(AuthScope, Credentials)]]
}

trait BlockingHttp extends HttpExecutor with BlockingCallback {
  import util.control.{Exception => Exc}
  /** This reference's underlying value is updated for the current thread when a
   *  request specifies credentials. Typically passed to ConfiguredHttpClient. */
  val credentials = new Http.CurrentCredentials(None)
  /** Reference to the underlying client. Override make_client define your own. */
  final val client = make_client
  /** Defaults to dispatch.ConfiguredHttpClient(credentials), override to customize. */
  def make_client: HttpClient = new ConfiguredHttpClient(credentials)

  /** Execute method for the given host. */
  private def execute(host: HttpHost, req: HttpRequestBase): HttpResponse = {
    client.execute(host, req)
  }
  /** Execute for given optional parameters, with logging. Creates local scope for credentials. */
  def execute[T](host: HttpHost,
                 credsopt: Option[Credentials], 
                 req: HttpRequestBase,
                 block: HttpResponse => T,
                 listener: ExceptionListener) =
    pack(req, try {
      block(credsopt.map { creds =>
        credentials.withValue(Some((
          new AuthScope(host.getHostName, host.getPort), creds)
        ))(execute(host, req))
      } getOrElse { execute(host, req) })
    } catch {
      case e => listener.lift(e); throw e
    })
  /** Potentially wraps payload, e.g. in a Future */
  def pack[T](req: { def abort() }, result: => T): HttpPackage[T]

  /** Shutdown connection manager, threads. */
  def shutdownClient() = client.getConnectionManager.shutdown()
}
