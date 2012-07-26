package dispatch.foursquare
import dispatch._

object Auth {
  private val svc: Request = :/ ("api.foursquare.com") secure

  def apply(version: String, creds: Pair[String, String], request: Request): Request = {
    (svc / version) <& request <<? Map("client_id" -> creds._1, "client_secret" -> creds._2)
  }

}
