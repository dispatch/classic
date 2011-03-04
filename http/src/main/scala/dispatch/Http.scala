package dispatch

import collection.Map
import collection.immutable.{Map => IMap}
import java.net.URI

import org.apache.http.{HttpHost,HttpRequest,HttpResponse,HttpEntity}
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
  def pack[T](req: HttpRequestBase, result: => T) = result
}

/** May be used directly from any thread. */
object Http extends Http with thread.Safety 

trait BlockingHttp extends HttpExecutor with BlockingCallback {
  val client = make_client
  /** Defaults to dispatch.ConfiguredHttpClient, override to customize. */
  def make_client = new ConfiguredHttpClient

  def exception[T](e: Exception) = throw(e)
  
  /** Execute method for the given host. */
  private def execute(host: HttpHost, req: HttpRequestBase): HttpResponse = {
    client.execute(host, req)
  }
  /** Execute for given optional parametrs, with logging. Creates local scope for credentials. */
  def execute[T](host: HttpHost, credsopt: Option[Credentials], 
                 req: HttpRequestBase, block: HttpResponse => T) =
    pack(req, block(
      credsopt.map { creds =>
        client.credentials.withValue(Some((
          new AuthScope(host.getHostName, host.getPort), creds)
        ))(execute(host, req))
      } getOrElse { execute(host, req) }
    ))
  def pack[T](req: HttpRequestBase, result: => T): HttpPackage[T]
}
