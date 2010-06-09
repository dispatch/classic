package dispatch

import org.apache.http.params.HttpParams
import org.apache.http.conn.ClientConnectionManager

class AppEngineConfiguredClient(conman: ClientConnectionManager) extends ConfiguredHttpClient(conman) {
  // NOOP
  override protected def configureProxy(params: HttpParams) = params
}

object AppEngineHttp {
  import dispatch.gae._
  val gae_connection_manager = new GAEConnectionManager
}

class AppEngineHttp extends Http {
  import AppEngineHttp._
  override val client = new AppEngineConfiguredClient(gae_connection_manager)
}
