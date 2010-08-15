package dispatch.google.cl

import dispatch._
import dispatch.Http._

/* http://code.google.com/apis/accounts/docs/AuthForInstalledApps.html */

sealed abstract class AccountType(val name: String)
case object Google extends AccountType("GOOGLE")
case object Hosted extends AccountType("HOSTED")
case object Ambiguous extends AccountType("HOSTED_OR_GOOGLE")

case class AuthRequest(service: String, email: String, password: String, a_type: AccountType, source: String) {
  def as_map = Map(
    "accountType" -> a_type.name,
    "Email"       -> email,
    "Passwd"      -> password,
    "service"     -> service,
    "source"      -> source
  )
}
object AuthRequest {
  def apply(service: String, email: String, password: String): AuthRequest =
    AuthRequest(service, email, password, Ambiguous, "dispatch")
}

case class Token(value: String)

object ClientLogin {
  val host = :/("www.google.com")
  val svc = host.secure / "accounts" / "ClientLogin"
  
  /* Extract an authorized token from a response source. */
  def auth_token(src: io.Source): Option[Token] =
    src.getLines.find { _ startsWith "Auth" } map { l => Token(l drop 5) }
  
  class RequestSigner(r: Request) {
    def <@(token: Token) = r <:< Map("Authorization" -> ("GoogleLogin auth=" + token.value))
  }
  
  implicit def AuthRequest2Request(a_req: AuthRequest) = svc << a_req.as_map
  
  implicit def Request2RequestSigner(r: Request) = new RequestSigner(r)
  
  //TODO captcha ..
  
}