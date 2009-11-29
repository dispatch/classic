package dispatch.twitter
import dispatch._

import json._
import JsHttp._
import oauth._
import oauth.OAuth._

object Twitter {
  val host = :/("twitter.com")
  val search = :/("search.twitter.com")
}

object Search extends Js {
  def apply(query: String, params: (String, Any)*) = new SearchBuilder(Map(params: _*)).q(query)
  
  class SearchBuilder(params: Map[String, Any]) extends Builder[Handler[List[JsObject]]] {
    private def param(key: String)(value: Any) = new SearchBuilder(params + (key -> value))
    
    val q = param("q")_
    val lang = param("lang")_
    val rpp = param("rpp")_
    val page = param("page")_
    val since_id = param("since_id")_
    private def geocode0(unit: String)(lat: Double, lon: Double, radius: Double) = 
      param("geocode")(List(lat, lon, radius).mkString(",") + unit)
    val geocode = geocode0("km")_
    val geocode_mi = geocode0("mi")_
    def product = Twitter.search / "search.json" <<? params ># ('results ! (list ! obj))
  }
  
  val to_user_id = 'to_user_id ? num
  val from_user_id = 'from_user_id ? num
  val source = 'source ? str
  val id = 'id ? num
  val text = 'text ? str
  val created_at = 'created_at ? str
  val iso_language_code = 'iso_language_code ? str
  val from_user = 'from_user ? str
}

object Status extends Request(Twitter.host / "statuses") {
  private def public_timeline = this / "public_timeline.json" ># (list ! obj)
  
  def friends_timeline(consumer: Consumer, token: Token, params: (String, Any)*) = 
    new FriendsTimelineBuilder(consumer, token, Map(params: _*))

  class FriendsTimelineBuilder(consumer: Consumer, token: Token, params: Map[String, Any]) 
      extends Builder[Handler[List[JsObject]]] {
  
    private def param(key: String)(value: Any) =
      new FriendsTimelineBuilder(consumer, token, params + (key -> value))
    val since_id = param("since_id")_
    val max_id = param("max_id")_
    val count = param("count")_
    val page = param("page")_
    def product =  Status / "friends_timeline.json" <<? params <@ (consumer, token) ># (list ! obj)
  }

  def update(status: String, consumer: Consumer, token: Token) =
    this / "update.json" << Map("status" -> status) <@ (consumer, token)
  
  val text = 'text ? str
  val id = 'id ? num
  val user = new Obj('user) with UserProps // Obj assigns context to itself

  def rebracket(status: String) = status replace ("&gt;", ">") replace ("&lt;", "<")
}

case class Status(user_id: String) extends 
    Request(Status / "user_timeline" / (user_id + ".json")) with Js {

  def timeline = this ># (list ! obj)
}

object User extends UserProps with Js // Js assigns context of None

trait UserProps {
  // undefined local context for `?` to allow Obj / Js to assign themselves
  implicit val ctx: Option[Obj]
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

  def request_token(consumer: Consumer, oauth_callback: String) = 
    svc.secure / "request_token" << 
      Map("oauth_callback" -> oauth_callback) <@ consumer as_token
    
  def authorize_url(token: Token) = svc / "authorize" <<? token
  def authenticate_url(token: Token) = svc / "authenticate" <<? token
  
  def access_token(consumer: Consumer, token: Token, verifier: String) = 
    svc.secure.POST / "access_token" <@ (consumer, token, verifier) >% { m =>
      (Token(m).get, m("user_id"), m("screen_name"))
    }
}
