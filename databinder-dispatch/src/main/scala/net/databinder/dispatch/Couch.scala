package net.databinder.dispatch.couch

import java.io.InputStream
import java.net.URLEncoder.encode
import org.apache.http.HttpHost

case class Doc(js: Js) extends JsT {
  lazy val _id = js(Symbol("_id"))(str)
  lazy val _rev = js(Symbol("_rev"))(str)
}

object Couch {
  def apply(host: String) = new Http(host, 5984)
  def apply(): Http = Couch("127.0.0.1")
}

case class Database(name: String) extends JsT {
  class H(val http: Http) extends Database(name) {
    def apply[T](id: String)(doc: Js => T) =
      http("/" + name + "/" + encode(id)) >> { str => doc(Js(str)) }
    def all_docs =
      this("_all_docs") { _('rows)(list(obj)).map { _('id)(str) } }
  }
  def apply(http: Http) = new H(http)
}
/*
object Revise extends Schema {
  val id = String('id)
  val rev = String('rev)

  def update(stream: InputStream, source: Store) =
    (source << Doc._rev)(new Store(stream)(rev))
    
  def apply(source: Store) = new {
    def <<: (req: Http#Request) =
      (req <<< source.toString) >> (update(_, source))
  }
}
*/