package dispatch.twitter

import dispatch._
import dispatch.oauth._
import OAuth._
import net.liftweb.json.{JsonAST,JsonParser}
import JsonAST._
import java.io.{InputStreamReader,BufferedReader}

object UserStream {
  val host = :/("userstream.twitter.com").secure
  val svc = host / "2" / "user.json"
  type Listener = JValue => Unit
  def open(cons: Consumer, tok: Token, since_id: Option[String])
          (make_listener: JValue => Listener) = {
    val req = svc <<? (Map.empty ++ since_id.map { id => "since_id" -> id })
    req <@ (cons, tok) >> { (stm, charset) =>
      val reader = new BufferedReader(new InputStreamReader(stm, charset))
      val listener = make_listener(JsonParser.parse(reader.readLine()))
      while (true) {
        listener(JsonParser.parse(reader.readLine()))
      }
    }
  }
}
