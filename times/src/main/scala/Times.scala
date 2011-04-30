package dispatch.times
import dispatch._

import json._
import Js._

@deprecated("This module will be discontinued. Take the source if you want it!")
trait Times extends JsHttp {
  import Http._
  lazy val host = :/("api.nytimes.com")
  val api_key: String
  val service: String
  val version: Int
  
  def apply(action: String, params: Map[String, String]) =
    /("svc") / service / ("v" + version) / action <<? (params + ("api-key" -> api_key))

  def apply(action: String): Request = this(action, Map.empty)

  val results = ('results ! (list ! obj))
}

@deprecated("This module will be discontinued. Take the source if you want it!")
case class People(api_key: String) extends Times {
  val service = "timespeople/api";
  val version = 1
  
  def profile(user_id: Int) = this("/user/" + user_id + "/profile.js")
}

@deprecated("This module will be discontinued. Take the source if you want it!")
case class Search(api_key: String) extends Times {
  val service = "search"
  val version = 1
  
  def search(query: String) = this("article", Map("query" -> query))
}

@deprecated("This module will be discontinued. Take the source if you want it!")
case class Community(api_key: String) extends Times {
  val service = "community"
  val version = 2

  override val results: JsValue => List[JsObject] = ('results ! obj) andThen ('comments ! (list ! obj))
  
  def recent = this("comments/recent.json") ># results
}


@deprecated("This module will be discontinued. Take the source if you want it!")
case class News(api_key: String) extends Times {
  val service = "news"
  val version = 2
  
  def recent = this("all/recent.json") ># results
}
