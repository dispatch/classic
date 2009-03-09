package dispatch.json.couch

import java.io.InputStream
import java.net.URLEncoder.encode
import org.apache.http.HttpHost

import json._
/*
trait Doc extends Js {
  val _id = Symbol("_id") ? str
  val _rev = Symbol("_rev") ? str
}
object Doc extends Doc

object Couch {
  def apply(host: String) = new Http(host, 5984)
  def apply(): Http = Couch("127.0.0.1")
}

case class Database(name: String) extends Js {
  class H(val http: Http) extends Database(name) {
    def apply(id: String): Http#Request = http("/" + name + "/" + encode(id))
    def all_docs =
      this("_all_docs") $ ('rows ! list(obj)) map ('id ! str)
  }
  def apply(http: Http) = new H(http)
}
*/
/*
object Revise extends JsDef {
  val id = 'id as str
  val rev = 'rev as str

  def update(stream: InputStream, source: Js) =
    source(Doc._rev << Js(stream)(rev))
    
  def apply(source: Js) = new {
    def <<: (req: Http#Request) =
      (req <<< source.toString) >> (update(_, source))
  }
}
*/
