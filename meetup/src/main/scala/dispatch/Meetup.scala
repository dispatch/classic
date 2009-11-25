package dispatch.meetup
import dispatch._

import oauth._
import oauth.OAuth._

object Meetup {
  val host = :/("api.meetup.com")
  val svc = host
}

case class Group(consumer: Consumer, access: Token) {
  val query = new GroupsBuilder(Map())
  def query(params: (String, Any)*) = new GroupsBuilder(Map(params: _*))
  class GroupsBuilder(params: Map[String, Any]) extends Builder[Handler[String]] {
    private def param(key: String)(value: Any) = new GroupsBuilder(params + (key -> value))

    val zip = param("zip")_
    def product = Meetup.svc / "groups.json" <<? params <@ (consumer, access) as_str
  }
}

object Auth {
  val svc = :/("www.meetup.com") / "oauth"

  def request_token(consumer: Consumer) = 
    svc.POST / "request/" <@ consumer as_token

  def request_token(consumer: Consumer, oauth_callback: String) = 
    svc / "request/" << 
      Map("oauth_callback" -> oauth_callback) <@ consumer as_token
    
  def authorize_url(token: Token) = :/("www.meetup.com") / "authorize/" <<? token
  
  def access_token(consumer: Consumer, token: Token) = 
    svc.POST / "access/" <@ (consumer, token) as_token
}
