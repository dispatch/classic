import scala.util.parsing.json._

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

class RichHttpClient(host: String, port: int) extends HttpClient {
  getHostConfiguration.setHost(host, port)
  
  def ? (m: HttpMethod)(fail: (Int)=>String) = try { executeMethod(m) match {
      case 200 => m.getResponseBodyAsString()
      case code => fail(code)
  } } finally { m.releaseConnection() }
  
  def ! (m: HttpMethod) = (this ? m)(code => error("Response not OK: " + code))
  
  def apply(uri: String) = this ! new GetMethod(uri)
}

val couch = new RichHttpClient("localhost", 5984)

class JsObject(store: Option[List[Any]]) {
  def this(json: String) = this(JSON parse json)
  
  private def resolve[T](s: Symbol)(fetch: (Any) => T) = store flatMap (_ find  (_ match {
    case (s.name, value: Any) => true
    case _ => false
  }) map fetch)
  
  def string(s: Symbol) = resolve(s)(_ match { case (_, value: String) => value })
  def number(s: Symbol) = resolve(s)(_ match { case (_, value: Number) => value })
}

class Person(json: String) extends JsObject(json) {
  def name = string('name)
  def age = number('age)
}