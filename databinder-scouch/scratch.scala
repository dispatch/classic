import scala.util.parsing.json._

import java.io.InputStream

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

class RichHttpClient(host: String, port: int) extends HttpClient {
  getHostConfiguration.setHost(host, port)
  
  def ? [T] (m: HttpMethod)(okay: (InputStream) => T)(fail: (Int, HttpMethod)=>T) = 
    try { executeMethod(m) match {
        case 200 => okay(m.getResponseBodyAsStream())
        case code => fail(code, m)
      } 
    } finally { m.releaseConnection() }
  
  def ! [T] (m: HttpMethod)(okay: (InputStream) => T) = 
    (this ? m)(okay){ (code, method) => error("Response not OK: " + code) }
  
  def apply[T](uri: String)(okay: (InputStream) => T) = (this ! new GetMethod(uri))(okay)
}

val couch = new RichHttpClient("localhost", 5984)

implicit def stream2reader(input: InputStream) = 
  scala.util.parsing.input.StreamReader(new java.io.InputStreamReader(input))

object JSON extends Parser {
  def parse(input: scala.util.parsing.input.Reader[Char]) =
    phrase(root)(new lexical.Scanner(input)) match {
      case Success(list: List[Tuple2[String, Any]], _) => mapify(list head, list tail)
      case _ => Map[String, Option[Any]]()
    }

  def mapify(tup: (String, Any), list: List[Tuple2[String, Any]]): Map[String, Option[Any]] =
    (list match {
      case Nil => Map[String, Option[Any]]()
      case _ => mapify(list head, list tail)
    }) + (tup._1 -> resolve(tup._2))

  def listify(value: Any, list: List[Any]): List[Any] = 
    resolve(value) :: (list match {
      case Nil => Nil
      case list => listify(list head, list tail)
    })

  def resolve(value: Any) = value match {
    case list: List[Any] => Some(list.head match {
      case tup:(String, Any) => mapify(tup, (list tail).asInstanceOf[List[Tuple2[String, Any]]])
      case value => listify(value, list tail)
    })
    case null => None
    case value => Some(value)
  }
}

trait JsObject {
  val store = load
  def load: Map[String,Option[Any]]
  
  private def resolve[T](s: Symbol)(fetch: (Any) => T) = fetch(store(s.name))
  
  def string(s: Symbol) = resolve(s) { case value: Option[String] => value }
  def number(s: Symbol) = resolve(s) { case value: Option[Number] => value }
  def list[T](s: Symbol) = resolve(s) { case value: Option[List[T]] => value }
}

trait Person extends JsObject {
  def name = string('name)
  def age = number('age)
  def pets = list[Any]('pets)
}