package dispatch.twitter

import json._
import Js._
import oauth._
import oauth.OAuth._

object Twitter {
  val host = :/("twitter.com")
  val search = :/("search.twitter.com")
}

case class Search(query: String, rpp: Int) extends 
    Request( Twitter.search / "search.json" <<? Map("q" -> query, "rpp" -> rpp.toString)  ) with Js {
  
  def results = this ># ('results ! (list ! obj))
}

object Search extends Js {
  def apply(query: String): Search = Search(query, 20)
  val to_user_id = 'to_user_id ? num
  val from_user_id = 'from_user_id ? num
  val source = 'source ? str
  val id = 'id ? num
  val text = 'text ? str
  val created_at = 'created_at ? str
  val iso_language_code = 'iso_language_code ? str
  val from_user = 'from_user ? str
}

object Status extends Request(Twitter.host / "statuses") with Js {
  def public_timeline = this / "public_timeline.json" ># (list ! obj)
  
  def friends_timeline(consumer: Consumer, token: Token) =
    this / "friends_timeline.json" <@ (consumer, token) ># (list ! obj)
  
  val text = 'text ? str
}

case class Status(user: String) extends 
    Request(Status / "user_timeline" / (user + ".json")) with Js {

  def timeline = this ># (list ! obj)
}

object User extends Js {
  val followers_count = 'followers_count ? num
  val screen_name = 'screen_name ? str
}

case class User(user: String) extends 
    Request(Twitter.host / "users" / "show" / (user + ".json")) with Js {

  def show = this ># obj
}

object Auth {
  
  val svc = Twitter.host / "oauth"

  def request_token(consumer: Consumer) = 
    svc.secure.POST / "request_token" <@ consumer as_token
    
  def authorize_url(token: Token) = svc / "authorize" <<? token
  
  def access_token(consumer: Consumer, token: Token, verifier: String) = 
    svc.secure.POST / "access_token" <@ (consumer, token, verifier) >% { m =>
      (Token(m), m("user_id"), m("screen_name"))
    }
}
