package dispatch.meetup

import dispatch._
import net.liftweb.json.JsonAST.JValue
import dispatch.liftjson.Js._

object RsvpsStream {
  val host = :/("stream.meetup.com")
  val svc = host / "2" / "rsvps" <:< Map("User-Agent" -> "dispatch-meetup")
  def open(since_mtime: Option[Long])
          (listener: JValue => Unit) =
    svc <<? (Map.empty ++ since_mtime.map {
      t => "since_mtime" -> t.toString
    }) ^# listener
}
