import org.specs._

object MeetupSpec extends Specification {
  import dispatch._
  import meetup._
  import dispatch.liftjson.Js._
  import oauth._

  val conf = new java.io.File("meetup.test.properties")
  if (conf.exists) {
    val config = {
      val stm = new java.io.FileInputStream(conf)
      val props = new java.util.Properties
      props.load(stm)
      stm.close()
      props
    }
    val consumer = Consumer(config.getProperty("oauth_consumer_key"), config.getProperty("oauth_consumer_secret"))
    val token = Token(config.getProperty("oauth_token"), config.getProperty("oauth_token_secret"))
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
        val (res, meta) = Http(client.handle(Events.group_id(1377720)
          .after(cal.getTime)
          .before(new java.util.Date)
        ))
        res.size must be > (5)
        (meta >>= Meta.count) must_== List(res.size)
      }
      "find upcoming events" in {
        val (res, meta) = Http(client.handle(Events.topic("technology")))
        val statuses = res flatMap Event.status
        statuses must notBeEmpty
        statuses must notExist { _ != Event.Upcoming }
      }
    }
    "Member and Group query" should {
      implicit val http = new Http
      "find NYSE members" in {
        val NYSE = "ny-scala"
        val (res, meta) = client.call(Members.group_urlname(NYSE))
        val ids = for (r <- res; id <- Member.id(r)) yield id
        ids.size must be > (5)
      }
    }
    "Photos query" should {
      implicit val http = new Http
      "Find North East Scala Symposium photos" in {
        val (res, _) = client.call(Photos.event_id("15526582"))
        val photos = for {
          r <- res
          id <- Photo.photo_id(r)
          created <- Photo.created(r)
          updated <- Photo.updated(r)
          hr_link <- Photo.highres_link(r)
          photo_link <- Photo.photo_link(r)
          thumb_link <- Photo.thumb_link(r)
        } yield (id, created, updated, hr_link, photo_link, thumb_link)
        photos.size must be > 5
      }
    }
  }
}
