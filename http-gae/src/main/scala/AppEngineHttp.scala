package dispatch.classic.gae

import org.apache.http.params.HttpParams
import org.apache.http.conn.ClientConnectionManager

class AppEngineConfiguredClient(
  credentials: dispatch.classic.Http.CurrentCredentials
) extends dispatch.classic.ConfiguredHttpClient(credentials) {
  /** @return GAEConnectionManager for non-socket based connections */
  override def createClientConnectionManager = GAEConnectionManager
  /** No need for proxy support on app engine. */
  override protected def configureProxy(params: HttpParams) = params
}

object Http extends Http

class Http extends dispatch.classic.Http {
  override def make_client = new AppEngineConfiguredClient(credentials)
}
