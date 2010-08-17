package dispatch.google.cl

import org.specs._
import dispatch._
import dispatch.Http._
import ClientLogin._

object ClientLoginSpec extends Specification {
  
  val conf = new java.util.Properties
  conf.load(new java.io.FileInputStream("google.clientlogin.props"))
  val email = conf.getProperty("email")
  val password = conf.getProperty("password")
  
  "Authorized request for contacts" should {
    "find contacts" in {
      val http = new Http
      val auth_req = AuthRequest("cp", email, password)
      
      val t = http(auth_req authorizer)
      val res = http(:/("www.google.com") / "m8" / "feeds" / "contacts" / auth_req.email / "full" <@ t <> { _ \\ "feed" })
      
      res must notBeEmpty
      res map { _ \ "entry" must notBeEmpty }
      res map { feed => (feed \ "id" text) must_== email }
    }

  }
}