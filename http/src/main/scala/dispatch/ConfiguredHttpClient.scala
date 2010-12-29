package dispatch

import util.DynamicVariable
import org.apache.http.{HttpHost,HttpVersion,HttpResponse}
import org.apache.http.auth.AuthScope
import org.apache.http.impl.client.{DefaultHttpClient, BasicCredentialsProvider}
import org.apache.http.conn.params.ConnRouteParams
import org.apache.http.params.{HttpProtocolParams, BasicHttpParams, HttpParams}

/** Basic extension of DefaultHttpClient defaulting to Http 1.1, UTF8, and no Expect-Continue.
    Scopes authorization credentials to particular requests thorugh a DynamicVariable. */
class ConfiguredHttpClient extends DefaultHttpClient { 
  protected def configureProxy(params: HttpParams) = {
    val sys = System.getProperties()
    val host = sys.getProperty("https.proxyHost", sys.getProperty("http.proxyHost"))
    val port = sys.getProperty("https.proxyPort", sys.getProperty("http.proxyPort"))
    if (host != null && port != null)
      ConnRouteParams.setDefaultProxy(params, new HttpHost(host, port.toInt))
    params
  }

  override def createHttpParams = {
    val params = new BasicHttpParams
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
    HttpProtocolParams.setContentCharset(params, Request.factoryCharset)
    HttpProtocolParams.setUseExpectContinue(params, false)
    configureProxy(params)
  }
  /** Follow response redirect regardless of request method */
  override def createRedirectHandler = new org.apache.http.impl.client.DefaultRedirectHandler {
    import org.apache.http.protocol.HttpContext
    import org.apache.http.HttpStatus._
    override def isRedirectRequested(res: HttpResponse, ctx: HttpContext) =
      (SC_MOVED_TEMPORARILY :: SC_MOVED_PERMANENTLY :: SC_TEMPORARY_REDIRECT :: 
       SC_SEE_OTHER :: Nil) contains res.getStatusLine.getStatusCode
  }
  val credentials = new DynamicVariable[Option[(AuthScope, Credentials)]](None)
  setCredentialsProvider(new BasicCredentialsProvider {
    override def getCredentials(scope: AuthScope) = credentials.value match {
      case Some((auth_scope, Credentials(n, p))) if scope.`match`(auth_scope) >= 0 =>
        new org.apache.http.auth.UsernamePasswordCredentials(n, p)
      case _ => null
    }
  })
}

