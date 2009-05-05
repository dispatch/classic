package dispatch.couch

import java.io.InputStream
import org.apache.http.HttpHost

import json._

/** Extractors for CouchDB document id and revsion properties.
    Extend with your own document properties. */
trait Id extends Js {
  val _id = Symbol("_id") ? str
  val _rev = Symbol("_rev") ? str
}
/** Extractors for CouchDB document id and revsion properties.
    Use this object for direct access to Id extractors. */
object Id extends Id

case class Couch(hostname: String, port: Int) extends Request(:/(hostname, port))

object Couch {
  def apply(): Couch = this("127.0.0.1")
  def apply(hostname: String): Couch = Couch(hostname, 5984)
}


/** Requests on a particular database. */
case class Db(couch: Couch, name: String) extends Request(couch / name) with Js {
  val all_docs =  this / "_all_docs" ># { 'rows ! (list ! obj) andThen { _ map ('id ! str) } }

  val create = this <<< Nil >|
  val delete = this <--() >|
}

import java.net.URLEncoder.encode

/** Requests on a particular document in a particular database. */
case class Doc(val db: Db, val id: String) extends Request(db / encode(id)) with Js {
  def update(js: JsValue) = this <<< js ># { 
    case Updated.rev(rev) => (Id._rev << rev)(js)
  }
  private object Updated { val rev = 'rev ? str }
  def delete(rev: String) = this <<? Map("rev" -> rev)  <--()  >|
}

