package dispatch.twitter

import json._

class SearchHttp extends Http("search.twitter.com")
class TwitterHttp extends Http("twitter.com")

object Search extends Js {
  val to_user_id = 'to_user_id ? num
  val from_user_id = 'from_user_id ? num
  val source = 'source ? str
  val id = 'id ? num
  val text = 'text ? str
  val created_at = 'created_at ? str
  val iso_language_code = 'iso_language_code ? str
  val from_user = 'from_user ? str

  def limit(rpp: Int)(q: String) = 
    /("search.json") <<? Map("q" -> q, "rpp" -> rpp.toString) ># (
      'results ! (list ! obj)
    )

  def apply(s: String) = limit(20)(s)
}

object Status extends Js {
  val user = new Obj('user) {
    val followers_count = 'followers_count ? num
    val screen_name = 'screen_name ? str
  }
  val text = 'text ? str

  val svc = /("statuses")
  
  def public_timeline = 
    svc / "public_timeline.json" ># (list ! obj)
  def user_timeline(user: String) =
    svc / "user_timeline" / (user + ".json") ># (list ! obj)
}

trait UserFields {
  import Js._
  val followers_count = 'followers_count ? num
  val screen_name = 'screen_name ? str
}

object User extends UserFields with Js {
  val svc = /("users")

  def show(user: String) = svc / "show" / (user + ".json") ># obj
}
