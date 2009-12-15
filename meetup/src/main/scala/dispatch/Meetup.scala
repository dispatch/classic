package dispatch.meetup
import dispatch._

import dispatch.oauth._
import dispatch.oauth.OAuth._

import dispatch.liftjson.Js._
import net.liftweb.json._
import net.liftweb.json.JsonAST._

import dispatch.mime.Mime._

import java.util.Date
import java.io.File

/** Client is a function to wrap API operations */
abstract class Client extends ((Request => Request) => Request) {
  import Http.builder2product
  val host = :/("api.meetup.com")
  def call[T](method: Method[T])(implicit http: Http) =
    http(method.default_handler(apply(method)))
}

trait Method[T] extends Builder[Request => Request] {
  /** default handler used by Client#call. You can also apply the client 
      to a Method and define your own request handler. */
  def default_handler: Request => Handler[T]
}

trait ReadMethod extends Method[(List[JValue],List[JValue])] {
  def default_handler = _ ># (Response.results ~ Response.meta)
}
trait WriteMethod extends Method[(List[String],List[String])] {
  def default_handler = _ ># (WriteResponse.description ~ WriteResponse.details)
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
  val host = :/("www.meetup.com")
  val svc = host / "oauth"

  def request_token(consumer: Consumer) = 
    svc.POST / "request/" <@ consumer as_token

  def authorize_url(token: Token) = host / "authorize/" <<? token
  def m_authorize_url(token: Token) = authorize_url(token) <<? Map("set_mobile" -> "on")
  def callback(oauth_callback: String) = Map("oauth_callback" -> oauth_callback)
  
  def access_token(consumer: Consumer, token: Token) = 
    svc.POST / "access/" <@ (consumer, token) as_token
}

object Response {
  val results = 'results ? ary
  val meta = 'meta ? obj
}
object WriteResponse {
  val description = 'description ? str
  val details = 'details ? str
}
/** Metadata returned with every API response */
object Meta {
  val count = 'count ? int
  val next = 'next ? str
  val total_count = 'total_count ? int
  val title = 'title ? str
  val updated = 'updated ? date
  val description = 'description ? str
  val method = 'method ? str
  val link = 'link ? str
  val url = 'url ? str
}
object Groups extends GroupsBuilder(Map())
private[meetup] class GroupsBuilder(params: Map[String, Any]) extends ReadMethod {
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

trait Location {
  val lat = 'lat ? str
  val lon = 'lon ? str
  val city = 'city ? str
  val state = 'state ? str
  val country = 'country ? str
  val zip = 'zip ? str
}

object Group extends Location {
  val name = 'name ? str
  val urlname = 'group_urlname ? str
  val group_photo_count = 'group_photo_count ? str
  val photo_url = 'photo_url ? str
  val link = 'link ? str
  val organizer_name = 'organizer_name ? str
  val who = 'who ? str
  val id = 'id ? str
  val topics = 'topics ? ary
  val organizerProfileURL = 'organizerProfileURL ? str
  val updated = 'updated ? date
  val created = 'created ? date
  val description = 'description ? str
  val rating = 'rating ? str
  val members = 'members ? str
  val daysleft = 'daysleft ? str
  
  object Topic {
    val id = 'id ? str
    val urlkey = 'urlkey ? str
    val name = 'name ? str
  }
}

object Events extends EventsBuilder(Map())
private[meetup] class EventsBuilder(params: Map[String, Any]) extends ReadMethod {
  private def param(key: String)(value: Any) = new EventsBuilder(params + (key -> value))

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

  def product = (_: Request) / "events" <<? params
}

object Event extends Location {
  val name = 'name ? str
  val id = 'id ? str
  val time = 'time ? date
  val description = 'description ? str
  val event_url = 'event_url ? str
  val photo_url = 'photo_url ? str
  val group_name = 'group_name ? str
  val group_photo_url = 'group_photo_url ? str
  val group_id = 'group_id ? str
  val attendee_count = 'attendee_count ? str
  val rsvpcount = 'rsvpcount ? str
  val no_rsvpcount = 'no_rsvpcount ? str
  val maybe_rsvpcount = 'maybe_rsvpcount ? str
  val rsvp_cutoff = 'rsvp_cutoff ? str
  val rsvp_closed = 'rsvp_closed ? str
  val rsvp_limit = 'rsvp_limit ? str
  val venue_name = 'venue_name ? str
  val venue_id = 'venue_id ? str
  val venue_address1 = 'venue_address1 ? str
  val venue_address2 = 'venue_address2 ? str
  val venue_address3 = 'venue_address3 ? str
  val venue_city = 'venue_city ? str
  val venue_state = 'venue_state ? str
  val venue_zip = 'venue_zip ? str
  val venue_phone = 'venue_phone ? str
  val venue_lat = 'venue_lat ? str
  val venue_lon = 'venue_lon ? str
  val venue_map = 'venue_map ? str
  val organizer_id = 'organizer_id ? str
  val organizer_name = 'organizer_name ? str
  val allow_maybe_rsvp = 'allow_maybe_rsvp ? str
  val myrsvp = 'myrsvp ? str
  val fee = 'fee ? str
  val feecurrency = 'feecurrency ? str
  val feedesc = 'feedesc ? str
  val ismeetup = 'ismeetup ? str
  val updated = 'updated ? date
  val questions = 'questions ? ary >>~> str 
}

object Members extends MembersBuilder(Map())
private[meetup] class MembersBuilder(params: Map[String, Any]) extends ReadMethod {
  private def param(key: String)(value: Any) = new MembersBuilder(params + (key -> value))

  val member_id = param("member_id")_
  def self = param("relation")("self")
  val group_id = param("group_id")_
  def topic(topic: Any, groupnum: Any) = param("topic")(topic).param("groupnum")(groupnum)
  val group_urlname = param("group_urlname")_

  def product = (_: Request) / "members" <<? params
}

object Member extends Location {
  val name = 'name ? str
  val id = 'id ? str
  val photo_url = 'photo_url ? str
  val link = 'link ? str
  val visited = 'visited ? date
  val joined = 'joined ? date
  val bio = 'bio ? str
}

object PhotoUpload extends PhotoUploadBuilder(None, Map())
private[meetup] class PhotoUploadBuilder(photo: Option[File], params: Map[String, Any]) extends WriteMethod {
  private def param(key: String)(value: Any) = new PhotoUploadBuilder(photo, params + (key -> value))

  val event_id = param("event_id")_
  def photo(photo: File) = new PhotoUploadBuilder(Some(photo), params)
  val caption = param("caption")_

  def product = (_: Request) / "photo" <<? params <<* ("photo", photo.getOrElse { error("photo not specified for PhotoUpload") } )
}
