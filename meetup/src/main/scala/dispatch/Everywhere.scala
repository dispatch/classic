package dispatch.meetup.everywhere
import dispatch.meetup.{Method, MethodBuilder, Response}
import dispatch._

import dispatch.liftjson.Js._
import net.liftweb.json._
import net.liftweb.json.JsonAST._

import dispatch.mime.Mime._

trait EverywhereMethod extends MethodBuilder {
  override def setup = (_: Request) / "ew"
}

trait ReadMethod extends dispatch.meetup.ReadMethod with EverywhereMethod
trait WriteMethod extends dispatch.meetup.WriteMethod with EverywhereMethod

object Containers extends ContainersMethod(Map.empty)
private[everywhere] class ContainersMethod(params: Map[String, Any]) extends ReadMethod {
  private def param(key: String)(value: Any) = new ContainersMethod(params + (key -> value))

  val urlname = param("urlname")_
  val id = param("id")_
  def complete = (_: Request) / "groups" <<? params
}