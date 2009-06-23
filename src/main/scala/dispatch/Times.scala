package dispatch.times

import json._
import Js._

trait Times extends Js {
  import Http._
  lazy val host = :/("api.nytimes.com")
  val api_key: String
  val service: String
  val version: Int
  
  def apply(action: String, params: Map[String, Any]) =
    /("svc") / service / ("v" + version) / action <<? (params + ("api-key" -> api_key))

  def apply(action: String): Request = this(action, Map[String, Any]())

  val results = ('results ! (list ! obj))
}

case class People(api_key: String) extends Times {
  val service = "timespeople/api";
  val version = 1
  
  def profile(user_id: Int) = this("/user/" + user_id + "/profile.js")
}

case class Search(api_key: String) extends Times {
  val service = "search"
  val version = 1
  
  def search(query: String) = this("article", Map("query" -> query))
}

case class Community(api_key: String) extends Times {
  val service = "community"
  val version = 2

  override val results: JsValue => List[JsObject] = ('results ! obj) andThen ('comments ! (list ! obj))
  
  def recent = this("comments/recent.json") ># results
}


case class News(api_key: String) extends Times {
  val service = "news"
  val version = 2
  
  def recent = this("all/recent.json") ># results
}
