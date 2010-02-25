import org.specs._

object MeetupSpec extends Specification {
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
    val nyseID = "05002008"

    "Group Query" should {
      "find knitting groups in Brooklyn" in {
        val http = new Http
        val group_topics = http(client(Groups.cityUS("Brooklyn", "NY").topic("knitting")) ># (
          Response.results >~> Group.topics
        ))
        group_topics.size must be > (0)
        group_topics forall { _.flatMap(Group.Topic.name) exists { _.toLowerCase == "knitting" } } must_== true
      }
    }
    "Event Query" should {
      implicit val http = new Http
      "find New York Scala events" in {
        import java.util.Calendar
        val cal = Calendar.getInstance
        cal.add(Calendar.YEAR, -1)
        val (res, meta) = client.call(Events.group_id(1377720)
          .after(cal.getTime)
          .before(new java.util.Date)
        )
        res.size must be > (5)
        (meta >>= Meta.count) must_== List(res.size)
      }
      "find upcoming events" in {
        val (res, meta) = client.call(Events.topic("technology"))
        val statuses = res flatMap Event.status
        statuses must notBeEmpty
        statuses must notExist { _ != Event.Upcoming }
      }
    }
    "Member and Group query" should {
      implicit val http = new Http
      "find NYSE members" in {
        val NYSE = "New-York-Scala-Enthusiasts"
        val (res, meta) = client.call(Members.group_urlname(NYSE))
        val ids = for (r <- res; id <- Member.id(r)) yield id
        ids.size must be > (5)
      }
    }
  }
}
