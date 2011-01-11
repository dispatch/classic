package dispatch.meetup

import dispatch._
import net.liftweb.json.JsonParser.parse
import net.liftweb.json.JsonAST.JValue
import scala.io.Source
import java.io.{InputStreamReader,BufferedReader}

object RsvpsStream {
  val host = :/("stream.meetup.com")
  val svc = host / "rsvps" <:< Map("User-Agent" -> "dispatch-meetup")
  def open(since_mtime: Option[Long])
          (listener: JValue => Unit) =
    svc <<? (Map.empty ++ since_mtime.map { t => "since_mtime" -> t }) >> {
      (stm, charset) =>
        Source.fromInputStream(stm, charset).getLines.filter {
          ! _.isEmpty
        }.map(parse).foreach(listener)
    }
}
