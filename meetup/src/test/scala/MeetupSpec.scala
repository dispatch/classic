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
      val http = new Http
      val groups = Groups(client)
      it("should find knitting groups in Brooklyn") {
        val group_topics = http(groups.cityUS("Brooklyn", "NY").topic("knitting") ># { jv =>
          Response.results(jv).map(Group.topics)
        })
        group_topics.size should be > (0)
        group_topics forall { _.flatMap(GroupTopic.name) exists { _.toLowerCase == "knitting" } } should be (true)
      }
    }
    describe("Event Query") {
/*      val http = new Http
      val events = Events(client)
      it("should find New York Scala events") {
        val res = http(events.group_id(1377720).after("05002008")) // nyc scala 4ever!
        res.results.size should be > (5)
      }*/
    }
  }
}
