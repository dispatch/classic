package dispatch.meetup
import dispatch._

import oauth._
import oauth.OAuth._

import dispatch.liftjson.Js._
import net.liftweb.json._
import net.liftweb.json.JsonAST._

import java.util.Date

/** Client is a function to wrap API operations */
abstract class Client extends ((Request => Request) => Request) {
  val host = :/("api.meetup.com")
}

/** Supplies a host and signs the request */
case class OAuthClient(consumer: Consumer, access: Token) extends Client {
  def apply(block: Request => Request): Request =
    block(host) <@ (consumer, access)
}
/** Supplies a host and adds an API key */
case class APIKeyClient(apikey: String) extends Client {
  def apply(block: Request => Request): Request =
    block(host) <<? Map("key" -> apikey)
}

/** Access point for tokens and authorization URLs */
object Auth {
  val svc = :/("www.meetup.com") / "oauth"

  def request_token(consumer: Consumer) = 
    svc.POST / "request/" <@ consumer as_token

  def request_token(consumer: Consumer, oauth_callback: String) = 
    svc / "request/" << 
      Map("oauth_callback" -> oauth_callback) <@ consumer as_token
    
  def authorize_url(token: Token) = :/("www.meetup.com") / "authorize/" <<? token
  
  def access_token(consumer: Consumer, token: Token) = 
    svc.POST / "access/" <@ (consumer, token) as_token
}

/** Metadata returned with every API response */
case class Meta(
  count: Int,
  next: String,
  total_count: Int,
  title: String,
  updated: Date,
  description: String,
  method: String,
  link: String,
  url: String
)
trait MeetupMethod {
  implicit val formats = new DefaultFormats {
    override def dateFormatter =
      new java.text.SimpleDateFormat("E MMM dd HH:mm:ss zz yyyy")
  }
}
object Groups extends MeetupMethod {
  def apply(client: Client) = {
    class GroupBuilder(params: Map[String, Any]) extends Builder[Handler[GroupResponse]] {
      private def param(key: String)(value: Any) = new GroupBuilder(params + (key -> value))

      val member_id = param("member_id")_
      val urlname = param("group_urlname")_
      val topic = param("topic")_
      def topic(topic: Any, groupnum: Any) = param("topic")(topic).param("groupnum")(groupnum)
      val id = param("id")_
      val zip = param("zip")_
      def geo(lat: Any, lon: Any) = param("lat")(lat).param("lon")(lon)
      def city(city: Any, country: Any) = param("city")(city).param("country")(country)
      def cityUS(city: Any, state: Any) = param("city")(city).param("state")(state).param("country")("us")
      val radius = param("radius")_

      private def order(value: String) = param("order")(value)
      def order_ctime = order("ctime")
      def order_name = order("name")
      def order_location = order("location")
      def order_members = order("members")
  
      def request = client((_: Request) / "groups" <<? params)
      def product = request ># { _.extract[GroupResponse] }
    }
    new GroupBuilder(Map()) 
  }
}

case class GroupResponse(results: List[Group], meta: Meta)
case class Group(
  name: String,
  group_photo_count: String,
  zip: String,
  lat: String,
  lon: String,
  photo_url: String,
  link: String,
  organizer_name: String,
  city: String,
  country: String,
  who: String,
  id: String,
  topics: List[GroupTopic],
  organizerProfileURL: String,
  updated: Date,
  created: Date,
  description: String,
  rating: String,
  members: String,
  daysleft: String
)
case class GroupTopic(id: String, urlkey: String, name: String)

