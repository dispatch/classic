package dispatch.twitter

import json._

trait Twitter extends Js {
  lazy val http = new Http("twitter.com")

  def apply(action: String, params: Map[String, Any]) = http(
    ("" :: service :: action :: Nil).mkString("/")
  ) ?< params

  def apply(action: String): Http#Request = this(action, Map[String, Any]())

  val service: String
}


object Status extends Js {
  val user = new Obj('user) {
    val followers_count = 'followers_count ? num
    val screen_name = 'screen_name ? str
  }
  val text = 'text ? str
}

class Statuses extends Twitter {
  val service = "statuses"
  
  def public_timeline = 
    this("public_timeline.json") $ (list ! obj)
  def user_timeline(user: String) =
    this("user_timeline/" + user + ".json") $ (list ! obj)
}