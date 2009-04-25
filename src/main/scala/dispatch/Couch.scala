package dispatch.couch

import java.io.InputStream
import org.apache.http.HttpHost

import json._

trait Doc extends Js {
  val _id = Symbol("_id") ? str
  val _rev = Symbol("_rev") ? str
}
object Doc extends Doc

object Couch {
  def apply(hostname: String): Http = new Http(hostname, 5984)
  def apply(): Http = apply("127.0.0.1")
}

case class Database(val name: String) extends Js {
  val base = /(name)
  val all_docs: Http => List[String] = _ { base / "_all_docs" ># ( 'rows ! (list ! obj) ) } map ('id ! str)

  val create = base <<< Nil >|
  val delete = base <--() >|
}

case class Document(val db: Database, val id: String) extends Js {
  val doc = db.base / java.net.URLEncoder.encode(id)
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
