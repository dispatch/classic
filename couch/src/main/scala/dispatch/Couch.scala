package dispatch.couch

import dispatch._
import json._
import JsHttp._
import org.apache.http.protocol.HTTP.UTF_8

/** Extractors for CouchDB document id and revsion properties.
    Extend with your own document properties. */
@deprecated("dispatch-couch is deprecated. See scouchdb for a more complete interface")
trait Id extends Js {
  val _id = Symbol("_id") ? str
  val _rev = Symbol("_rev") ? str
}
/** Extractors for CouchDB document id and revsion properties.
    Use this object for direct access to Id extractors. */
@deprecated("dispatch-couch is deprecated. See scouchdb for a more complete interface")
object Id extends Id

/** Factory for a CouchDB Request host with common parameters */
@deprecated("dispatch-couch is deprecated. See scouchdb for a more complete interface")
object Couch {
  def apply(hostname: String, port: Int): Request = :/(hostname, port)
  def apply(): Request = apply("127.0.0.1")
  def apply(hostname: String): Request = apply(hostname, 5984)
  /** A projection for the common { "rows": [{ "id": ... */
  val id_rows: JsF[List[String]] = ('rows ! list andThen { _ map 'id ! str })
}

/** Requests on a particular database and CouchDB host. */
@deprecated("dispatch-couch is deprecated. See scouchdb for a more complete interface")
case class Db(couch: Request, name: String) extends Js {
  val request = couch / name
  val all_docs =  request / "_all_docs" ># Couch.id_rows

  val create = request <<< "" >|
  val delete = request.DELETE >|
}

import java.net.URLEncoder.encode

/** Requests on a particular document in a particular database. */
@deprecated("dispatch-couch is deprecated. See scouchdb for a more complete interface")
class Doc(val db: Db, val id: String) extends Request(db.request / encode(id, UTF_8)) with Js {
  import Request._
  def update(js: JsValue) = this <<< js.toString ># { 
    case Updated.rev(rev) => (Id._rev << rev)(js)
  }
  private object Updated { val rev = 'rev ? str }
  def delete(rev: String) = this.DELETE <<? Map("rev" -> rev) >|
}

@deprecated("dispatch-couch is deprecated. See scouchdb for a more complete interface")
object Design {
  def apply(db: Db, design: String) = db.request / "_design" / design
  val views = 'views ? obj
}
@deprecated("dispatch-couch is deprecated. See scouchdb for a more complete interface")
object View {
  def apply(design: Request, view: String) = design / "_view" / view
  val map = 'map ? str
  val reduce = 'reduce ? str
}
