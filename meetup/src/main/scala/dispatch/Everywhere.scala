package dispatch.meetup.everywhere
import dispatch.meetup.{Method, MethodBuilder, Response}
import dispatch._

import dispatch.liftjson.Js._
import net.liftweb.json._
import net.liftweb.json.JsonAST._

import dispatch.mime.Mime._

import java.util.Date

trait EverywhereMethod extends MethodBuilder {
  override def setup = (_: Request) / "ew"
}

trait ReadMethod extends dispatch.meetup.ReadMethod with EverywhereMethod
trait WriteMethod extends dispatch.meetup.WriteMethod with EverywhereMethod

object Containers extends ContainersMethod(Map.empty)
private[everywhere] class ContainersMethod(params: Map[String, Any]) extends ReadMethod {
  private def param(key: String)(value: Any) = new ContainersMethod(params + (key -> value))

  val urlname = param("urlname")_
  val container_id = param("container_id")_
  val link = param("link")_
  def fields(fields: Iterable[String]) = param("fields")(fields.mkString(","))
  def complete = (_: Request) / "containers" <<? params
}

trait ContainerMethod extends WriteMethod {
  protected def param(key: String)(value: Any): ContainerMethod
  val description = param("description")_
  val link = param("link")_
  val link_name = param("link_name")_
  val facebook_urlname = param("facebook_urlname")_
  val twitter_urlname = param("twitter_urlname")_
  def founder_create = param("event_create")("founder")
  def anyone_create = param("event_create")("anyone")
  def time(date: Date) = param("time")(date.getTime)
  def open_scheduling = param("scheduling")("open")
  def date_scheduling = param("scheduling")("date")
  def datetime_scheduling = param("scheduling")("datetime")
}
object ContainerCreate {
  def apply(name: String) = new ContainerCreateMethod(Map("name" -> name))
}
private[everywhere] class ContainerCreateMethod(params: Map[String, Any]) extends ContainerMethod {
  protected def param(key: String)(value: Any) = new ContainerCreateMethod(params + (key -> value))
  def complete = (_: Request) / "container" << params
}
object ContainerEdit { def apply(id: Int) = new ContainerEditMethod(id, Map.empty) }
private[everywhere] class ContainerEditMethod(id: Int, params: Map[String, Any]) extends ContainerMethod {
  protected def param(key: String)(value: Any) = new ContainerEditMethod(id, params + (key -> value))
  val name = param("name")_
  override def complete = (_: Request) / "container" / id.toString << params
}

object Container {
  val id = 'id ? int
  val name = 'name ? str
  val meetup_url = 'meetup_url ? str
  val urlname = 'urlname ? str
  val description = 'description ? str
  val time = 'time ? date
  val scheduling = 'scheduling ? str
  val link = 'link ? str
  val link_name = 'link_name ? str
  val facebook_urlname = 'facebook_urlname ? str
  val twitter_urlname = 'twitter_urlname ? str
  val event_create = 'event_create ? str
  val meetup_count = 'meetup_count ? int
  val member_count = 'member_count ? int
  val created = 'created ? date
  val updated = 'updated ? date
}

object Events extends EventsMethod(Map.empty)
private[everywhere] class EventsMethod(params: Map[String, Any]) extends ReadMethod {
  private def param(key: String)(value: Any) = new EventsMethod(params + (key -> value))

  val event_id = param("event_id")_
  val urlname = param("urlname")_
  val container_id = param("container_id")_
  def geo(lat: BigDecimal, lon: BigDecimal) = param("lat")(lat).param("lon")(lon)
  def before(date: Date) = param("before")(date.getTime)
  def after(date: Date) = param("after")(date.getTime)
  def upcoming = param("status")("upcoming")
  def past = param("status")("past")
  def fields(fields: Iterable[String]) = param("fields")(fields.mkString(","))
  def complete = (_: Request) / "events" <<? params
}
trait EventMethod extends WriteMethod {
  protected def param(key: String)(value: Any): EventMethod
  def geo(lat: BigDecimal, lon: BigDecimal) = param("lat")(lat).param("lon")(lon)
  def city(city: String, country: String) = param("city")(city).param("country")(country)
  def cityUS(city: String, state: String) = param("city")(city).param("country")("us").param("state")(state)
  val zip = param("zip")_
  val address1 = param("address1")_
  def time(time: Date) = param("time")(time.getTime)
  val description = param("description")_
  val title = param("title")_
  val venue_name = param("venue_name")_
  def fields(fields: Iterable[String]) = param("fields")(fields.mkString(","))
}
object EventCreate {
  def apply(name: String) = new EventCreateMethod(Map.empty)
}
private[everywhere] class EventCreateMethod(params: Map[String, Any]) extends EventMethod {
  protected def param(key: String)(value: Any) = new EventCreateMethod(params + (key -> value))
  val urlname = param("urlname")_
  val container_id = param("container_id")_
  def complete = (_: Request) / "event" << params
}
object EventEdit { def apply(id: Int) = new EventEditMethod(id, Map.empty) }
private[everywhere] class EventEditMethod(id: Int, params: Map[String, Any]) extends EventMethod {
  protected def param(key: String)(value: Any) = new EventEditMethod(id, params + (key -> value))
  def organize(setting: Boolean) = param("organize")(setting)
  override def complete = (_: Request) / "event" / id.toString << params
}
