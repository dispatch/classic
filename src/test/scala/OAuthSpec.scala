import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class OAuthSpec extends Spec with ShouldMatchers {
  import dispatch._
  import oauth._
  import OAuth._
  
  val svc = :/("term.ie") / "oauth" / "example"
  val consumer = Consumer("key", "secret")
  
  describe("OAuth test host") {
    it("should echo parameters from protected service") {
      val h = new Http
      val request_token = h(svc / "request_token.php" <@ consumer as_token)
      val access_token = h(svc / "access_token" <@ (consumer, request_token) as_token)
      h(
        svc / "echo_api.php" <<? Map("top"->"secret") <@ (consumer, access_token) as_str
      ) should be ("top=secret")
    }
  }
}
