import scala.util.parsing.json._

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

class RichHttpClient(host: String, port: int) extends HttpClient {
  getHostConfiguration.setHost(host, port)
  
  def ? (m: HttpMethod)(fail: (Int)=>HttpMethod) = executeMethod(m) match {
      case 200 => m
      case code => fail(code)
  }
  def ! (m: HttpMethod) = (this ? m)(code => error("Response not OK: " + code))
}

val couch = new RichHttpClient("localhost", 5984)
