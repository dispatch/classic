package net.databinder.dispatch.times

trait Times {
  lazy val http = new Http("api.nytimes.com")
  val api_key: String
  val service: String
  val version: String
  
  def exec(action: String, params: Map[String, Any]) =
    http("/svc/" + service + "/api/" + version + action) ?< (params + ("api-key" -> api_key))

  def exec(action: String): Http#Request = exec(action, Map[String, Any]())
}

case class People(api_key: String) extends Times {
  val service = "timespeople";
  val version = "v1"
  
  def profile(user_id: Int) = exec("/user/" + user_id + "/profile.js");
}

case class Search(api_key: String) extends Times {
  val service = "search"
  val version = "v1"
  
  def search(query: String) = exec("/article", Map("query" -> query))
}
