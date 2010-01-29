import org.specs._

object TwitterSpec extends Specification {
  import dispatch._
  import Http._
  import json.Js._
  import twitter._
  
  "Twitter Search" should {
    val http = new Http
    "find tweets containing #scala" in {
      val res = http(Search("#scala"))
      res.isEmpty must beFalse
      res map Status.text forall { _.toLowerCase contains "#scala" } must beTrue
    }
  }
}
