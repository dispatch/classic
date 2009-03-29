import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class HttpSpec extends Spec with ShouldMatchers {
  import dispatch._

  val jane = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.\n"
  val tus = new Http("technically.us")
  
  describe("Singleton Http test get") {
    endpoint_tests(Http("http://technically.us/test.text"))
  }
  describe("Bound host get") {
    endpoint_tests(tus("/test.text"))
  }
  def endpoint_tests(test: Http#Request) = {
    it("should equal expected string") {
      test.as_str should equal (jane)
    }
    it("should stream to expected sting") {
      test >> { str => io.Source.fromInputStream(str).mkString } should equal (jane)
    }
  }
}