import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class MeetupSpec extends Spec with ShouldMatchers {
  import dispatch._
  import meetup._
  import oauth._
  import Http._
  
  val conf = new java.io.File("meetup.test.conf")
  if (conf.exists) {
    import _root_.net.lag.configgy.{Configgy => C}
    C.configure(conf.getPath)
    
    val consumer = Consumer(C.config.getString("oauth_consumer_key").get, C.config.getString("oauth_consumer_secret").get)
    val Some(token) = Token(C.config.asMap)
    val group = Group(consumer, token)

    describe("Group Query") {
      val http = new Http
      it("should find groups in Brooklyn") {
      }
    }
  }
}
