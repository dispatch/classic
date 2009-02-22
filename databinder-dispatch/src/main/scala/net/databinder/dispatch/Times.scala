package net.databinder.dispatch.times

trait Times {
  lazy val http = new Http("api.nytimes.com")
  val api_key: String
  val service: String
  val version: Int
  
  def exec(action: String, params: Map[String, Any]) =
    http(
      ("/svc" :: service :: "v" + version :: action :: Nil).mkString("/")
    ) ?< (params + ("api-key" -> api_key))

  def exec(action: String): Http#Request = exec(action, Map[String, Any]())
}

case class People(api_key: String) extends Times {
  val service = "timespeople/api";
  val version = 1
  
  def profile(user_id: Int) = exec("/user/" + user_id + "/profile.js");
}

case class Search(api_key: String) extends Times {
  val service = "search"
  val version = 1
  
  def search(query: String) = exec("/article", Map("query" -> query))
}

case class Community(api_key: String) extends Times {
  val service = "community"
  val version = 2
  
  def recent = exec("comments/recent.json")
}

