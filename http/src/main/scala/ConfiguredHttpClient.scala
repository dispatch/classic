package dispatch

import org.apache.http.{HttpHost,HttpVersion,HttpRequest,HttpResponse}
import org.apache.http.auth.{AuthScope, NTCredentials, UsernamePasswordCredentials}
import org.apache.http.impl.client.{DefaultHttpClient, BasicCredentialsProvider}
import org.apache.http.conn.params.ConnRouteParams
import org.apache.http.params.{HttpProtocolParams, BasicHttpParams, HttpParams}
import org.apache.http.client.params.AuthPolicy

/** Basic extension of DefaultHttpClient defaulting to Http 1.1, UTF8, and no Expect-Continue.
    Scopes authorization credentials to particular requests thorugh a DynamicVariable. */
class ConfiguredHttpClient(
  credentials: Http.CurrentCredentials
) extends DefaultHttpClient { 
  protected var proxyScope: Option[AuthScope] = None
  protected var proxyBasicCredentials: Option[UsernamePasswordCredentials] = None
  protected var proxyNTCredentials: Option[NTCredentials] = None
  protected def configureProxy(params: HttpParams) = {
    val sys = System.getProperties()
    val host = sys.getProperty("https.proxyHost", sys.getProperty("http.proxyHost"))
    val port = sys.getProperty("https.proxyPort", sys.getProperty("http.proxyPort"))
    val user = sys.getProperty("https.proxyUser", sys.getProperty("http.proxyUser"))
    val password = sys.getProperty("https.proxyPassword", sys.getProperty("http.proxyPassword"))
    val domain = sys.getProperty("https.auth.ntlm.domain", sys.getProperty("http.auth.ntlm.domain"))
    if (host != null && port != null) {
      ConnRouteParams.setDefaultProxy(params, new HttpHost(host, port.toInt))
      proxyScope = Some(new AuthScope(host, port.toInt))
    }
    if (user != null && password != null) {
      proxyBasicCredentials = Some(new UsernamePasswordCredentials(user, password))
      // We should pass our hostname, actually
      // Also, we ought to support "domain/user" syntax
      proxyNTCredentials = Some(new NTCredentials(user, password, "", Option(domain) getOrElse ""))
    }
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
  setRedirectStrategy(new org.apache.http.impl.client.DefaultRedirectStrategy {
    import org.apache.http.protocol.HttpContext
    import org.apache.http.HttpStatus._
    override def isRedirected(req: HttpRequest, res: HttpResponse, ctx: HttpContext) =
      (SC_MOVED_TEMPORARILY :: SC_MOVED_PERMANENTLY :: SC_TEMPORARY_REDIRECT :: 
       SC_SEE_OTHER :: Nil) contains res.getStatusLine.getStatusCode
  })
  setCredentialsProvider(new BasicCredentialsProvider {
    override def getCredentials(scope: AuthScope) = credentials.value flatMap {
      case (auth_scope, Credentials(n, p)) if scope.`match`(auth_scope) >= 0 =>
        Some(new UsernamePasswordCredentials(n, p))
      case _ => None
    } orElse {
      // This test probably returns true even if only the port matches
      if (proxyScope exists (scope.`match`(_) >= 0)) {
        if (scope.getScheme() == AuthPolicy.NTLM) proxyNTCredentials
        else proxyBasicCredentials
      } else None
    } orNull
  })
}

