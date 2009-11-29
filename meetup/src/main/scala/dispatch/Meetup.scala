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

object Response {
  val results = 'results ? ary
  val meta = 'meta ? obj
}
/** Metadata returned with every API response */
object Meta {
  val count = 'count ? int
  val next = 'next ? str
  val total_count = 'total_count ? int
  val title = 'title ? str
  val updated = 'updated ? str
  val description = 'description ? str
  val method = 'method ? str
  val link = 'link ? str
  val url = 'url ? str
}
object Groups extends GroupsBuilder(Map())
private[meetup] class GroupsBuilder(params: Map[String, Any]) extends Builder[Request => Request] {
  private def param(key: String)(value: Any) = new GroupsBuilder(params + (key -> value))

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

  def product = (_: Request) / "groups" <<? params
}

object Group {
  val name = 'name ? str
  val group_photo_count = 'group_photo_count ? str
  val zip = 'zip ? str
  val lat = 'lat ? str
  val lon = 'lon ? str
  val photo_url = 'photo_url ? str
  val link = 'link ? str
  val organizer_name = 'organizer_name ? str
  val city = 'city ? str
  val country = 'country ? str
  val who = 'who ? str
  val id = 'id ? str
  val topics = 'topics ? ary
  val organizerProfileURL = 'organizerProfileURL ? str
  val updated = 'updated ? str
  val created = 'created ? str
  val description = 'description ? str
  val rating = 'rating ? str
  val members = 'members ? str
  val daysleft = 'daysleft ? str
}
object GroupTopic {
  val id = 'id ? str
  val urlkey = 'urlkey ? str
  val name = 'name ? str
}
/*
object Events extends MeetupMethod {
  def apply(client: Client) = {
    class GroupBuilder(params: Map[String, Any]) extends Builder[Handler[EventResponse]] {
      private def param(key: String)(value: Any) = new GroupBuilder(params + (key -> value))

      val member_id = param("member_id")_
      val group_urlname = param("group_urlname")_
      val topic = param("topic")_
      def topic(topic: Any, groupnum: Any) = param("topic")(topic).param("groupnum")(groupnum)
      val group_id = param("group_id")_
      val zip = param("zip")_
      def geo(lat: Any, lon: Any) = param("lat")(lat).param("lon")(lon)
      def city(city: Any, country: Any) = param("city")(city).param("country")(country)
      def cityUS(city: Any, state: Any) = param("city")(city).param("state")(state).param("country")("us")
      val radius = param("radius")_
      val after = param("after")_
      val before = param("before")_

      private def order(value: String) = param("order")(value)
      def order_time = order("time")
      def order_group = order("group")
      def order_location = order("location")
      def order_topic = order("topic")
  
      def request = client((_: Request) / "events" <<? params)
      def product = request ># { _.extract[EventResponse] }
    }
    new GroupBuilder(Map()) 
  }
}
case class EventResponse(results: List[Event], meta: Meta)
class Event(
  val name: String,
  val id: String,
  val time: Date,
  val description: String,
  val event_url: String,
  val photo_url: String,
  val group_name: String,
  val group_photo_url: String,
  val group_id: String,
  val attendee_count: String,
  val rsvpcount: String,
  val no_rsvpcount: String,
  val maybe_rsvpcount: String,
  val rsvp_cutoff: String,
  val rsvp_closed: String,
  val rsvp_limit: String,
  val venue_name: String,
  val venue_id: String,
  val venue_address1: String,
  val venue_address2: String,
  val venue_address3: String,
  val venue_city: String,
  val venue_state: String,
  val venue_zip: String,
  val venue_phone: String,
  val venue_lat: String,
  val venue_lon: String,
  val venue_map: String,
  val organizer_id: String,
  val organizer_name: String,
  val allow_maybe_rsvp: String,
  val myrsvp: String,
  val fee: String,
  val feecurrency: String,
  val feedesc: Option[String],
  val ismeetup: String,
  val updated: Date,
  val lat: String,
  val lon: String,
  val questions: List[String]
) { override def toString = "Event: %s on %s" format (name, time) } */