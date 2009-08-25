import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class TwitterSpec extends Spec with ShouldMatchers {
  import dispatch._
  import Http._
  import json.Js._
  import twitter._
  
  describe("Twitter Search") {
    val http = new Http
    it("should find tweets containing #scala") {
      val res = http(Search("#scala"))
      res.isEmpty should be (false)
      res map Status.text forall { _.toLowerCase contains "#scala" } should be (true)
    }
  }
}
