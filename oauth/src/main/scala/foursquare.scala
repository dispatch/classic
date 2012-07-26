package dispatch.foursquare
import dispatch._

object Auth {
  private val svc: Request = :/ ("api.foursquare.com") secure

  def apply(creds: Pair[String, String])(request: Request): Request = {
    svc <& request <<? Map("client_id" -> creds._1, "client_secret" -> creds._2)
  }

}
