package dispatch.meetup

import json._
import JsHttp._
import oauth._
import oauth.OAuth._

object Meetup {
  val host = :/("api.meetup.com")
  val svc = host
}

object Group {
  val query = new GroupsBuilder(Map())
  def query(params: (String, Any)*) = new GroupsBuilder(Map(params: _*))
  class GroupsBuilder(params: Map[String, Any]) extends Builder[Handler[List[JsObject]]] {
    private def param(key: String)(value: Any) = new GroupsBuilder(params + (key -> value))

    val zip = param("zip")_
    def product = Meetup.svc / "groups.json" <<? params ># ('results ! (list ! obj))
  }
  val urlname = 'group_urlname ? str
}

object Auth {
  val svc = Meetup.host / "oauth"

  def request_token(consumer: Consumer) = 
    svc.secure.POST / "request" <@ consumer as_token

  def request_token(consumer: Consumer, oauth_callback: String) = 
    svc.secure / "request" << 
      Map("oauth_callback" -> oauth_callback) <@ consumer as_token
    
  def authorize_url(token: Token) = :/("www.meetup.com") / "authorize" <<? token
  
  def access_token(consumer: Consumer, token: Token) = 
    svc.secure.POST / "access" <@ (consumer, token) >% { m =>
      (Token(m).get, m("user_id"), m("screen_name"))
    }
}
