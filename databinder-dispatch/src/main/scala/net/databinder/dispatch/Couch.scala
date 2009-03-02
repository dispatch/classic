package net.databinder.dispatch.couch

import java.io.InputStream
import java.net.URLEncoder.encode
import org.apache.http.HttpHost

import net.databinder.dispatch._

trait Doc extends JsType {
  val _id = Str(Symbol("_id"))
  val _rev = Str(Symbol("_rev"))
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
      this("_all_docs") $ Lst(Obj('rows)) map Str('id)
  }
  def apply(http: Http) = new H(http)
}

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
