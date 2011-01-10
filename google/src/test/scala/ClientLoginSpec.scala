package dispatch.google.cl

import org.specs._
import dispatch._
import ClientLogin._

object ClientLoginSpec extends Specification {
  val config = "google.clientlogin.props"
  lazy val conf = {
    val c = new java.util.Properties
    c.load(new java.io.FileInputStream(config))
    c
  }
  def email = conf.getProperty("email")
  def password = conf.getProperty("password")
  
  "Authorized request for contacts" should {
    "find contacts" in {
      (new java.io.File(config)).exists must beTrue.orSkip
      val http = new Http
      val auth_req = AuthRequest("cp", email, password)
      
      val token = http(auth_req authorize)
      val res = http(:/("www.google.com") / "m8" / "feeds" / "contacts" / auth_req.email / "full" <@ token <> { _ \\ "feed" })
      
      res must notBeEmpty
      res map { _ \ "entry" must notBeEmpty }
      res map { feed => (feed \ "id" text) must_== email }
    }

  }
}
