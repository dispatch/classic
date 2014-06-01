package dispatch.classic.foursquare

import dispatch.classic._
import org.specs2.mutable.Specification

object FoursquareSpec extends Specification {

  private val req = /\ / "v2" / "venues" / "search" <<? Map("ll" -> "123.123,456.456", "v" -> "19700101")

  "A basic foursquare request that requires auth" should {
    "be able to be formed normally" in {
      val builtReq = Auth(("key", "secret"))(req).to_uri.toString

      builtReq.contains("api.foursquare.com/v2/venues/search") must_== true
      builtReq.contains("client_id=key") must_== true
      builtReq.contains("client_secret=secret") must_== true
      builtReq.contains("ll=123.123%2C456.456") must_== true
      builtReq.contains("v=19700101") must_== true
      builtReq.contains("https://") must_== true
    }

    "be able to be formed normally through other apply" in {
      val builtReq = Auth("key", "secret")(req).to_uri.toString

      builtReq.contains("api.foursquare.com/v2/venues/search") must_== true
      builtReq.contains("client_id=key") must_== true
      builtReq.contains("client_secret=secret") must_== true
      builtReq.contains("ll=123.123%2C456.456") must_== true
      builtReq.contains("v=19700101") must_== true
      builtReq.contains("https://") must_== true
    }
  }
}

