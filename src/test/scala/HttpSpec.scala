import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class HttpSpec extends Spec with ShouldMatchers {
  import dispatch._

  val jane = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.\n"
  val tus = new Http("technically.us")
  
  describe("Expected text") {
    it("should equal singleton fetched string") {
      Http("http://technically.us/test.text").as_str should equal (jane)
    }
    it("should equal bound-host fetched string") {
      tus("/test.text").as_str should equal (jane)
    }
  }
  
}