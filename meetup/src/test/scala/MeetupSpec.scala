import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class MeetupSpec extends Spec with ShouldMatchers {
  import dispatch._
  import meetup._
  import dispatch.liftjson.Js._
  import oauth._
  import Http._
  
  val conf = new java.io.File("meetup.test.cfg")
  if (conf.exists) {
    import _root_.net.lag.configgy.{Configgy => C}
    C.configure(conf.getPath)
    
    val consumer = Consumer(C.config.getString("oauth_consumer_key").get, C.config.getString("oauth_consumer_secret").get)
    val Some(token) = Token(C.config.asMap)
    val client = OAuthClient(consumer, token)

    describe("Group Query") {
      it("should find knitting groups in Brooklyn") {
        val http = new Http
        val group_topics = http(client(Groups.cityUS("Brooklyn", "NY").topic("knitting")) ># (
          Response.results >~> Group.topics
        ))
        group_topics.size should be > (0)
        group_topics forall { _.flatMap(Group.Topic.name) exists { _.toLowerCase == "knitting" } } should be (true)
      }
    }
    describe("Event Query") {
      implicit val http = new Http
      it("should find New York Scala events") {
        val (res, meta) = client.call(Events.group_id(1377720).after("05002008")) // nyc scala 4ever!
        res.size should be > (5)
      }
    }
  }
}
