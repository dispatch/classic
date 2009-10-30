package dispatch.meetup

import json._
import JsHttp._
import oauth._
import oauth.OAuth._

object Meetup {
  val host = :/("api.dev.meetup.com")
  val svc = host / "api"
}

object Group {
  val query = new GroupsBuilder(Map())
  def query(params: (String, Any)*) = new GroupsBuilder(Map(params: _*))
  class GroupsBuilder(params: Map[String, Any]) extends Builder[Handler[List[JsObject]]] {
    private def param(key: String)(value: Any) = new GroupsBuilder(params + (key -> value))

    def city(city: String, state: String) = param("city")(city).param("state")(state)
    def product = Meetup.svc / "groups.json" <<? params ># ('results ! (list ! obj))
  }
  val name = 'name ? str
}