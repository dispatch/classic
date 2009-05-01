import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class HttpSpec extends Spec with ShouldMatchers {
  import dispatch._
  import json.Js._
  import twitter._
  
  describe("Twitter Search") {
    val http = new SearchHttp
    it("should find tweets containing #dbDispatch") {
      val res = http(Search("#dbDispatch"))
      res.isEmpty should be (false)
      res map Status.text forall { _ contains "#dbDispatch" } should be (true)
    }
  }
}
