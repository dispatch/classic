package net.databinder.dispatch

trait Times {
  lazy val http = new Http("api.nytimes.com")
  val api_key: String
  val service: String
  val version: String
  
  def exec(action: String) =
    http("/svc/" + service + "/api/" + version + action + ".js?api-key=" + api_key)
}

case class People(api_key: String) extends Times {
  val service = "timespeople";
  val version = "v1"
  
  def profile(user_id: Int) = exec("/user/" + user_id + "/profile");
}
