package dispatch.gae

import org.apache.http.params.HttpParams
import org.apache.http.conn.ClientConnectionManager

class AppEngineConfiguredClient extends dispatch.ConfiguredHttpClient {
  /** @return GAEConnectionManager for non-socket based connections */
  override def createClientConnectionManager = GAEConnectionManager
  /** No need for proxy support on app engine. */
  override protected def configureProxy(params: HttpParams) = params
}

object Http extends Http

class Http extends dispatch.Http {
  override val client = new AppEngineConfiguredClient
}
