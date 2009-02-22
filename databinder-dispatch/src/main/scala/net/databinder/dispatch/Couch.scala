package net.databinder.dispatch.couch

import java.io.InputStream
import java.net.URLEncoder.encode
import org.apache.http.HttpHost

/*
trait Doc extends Schema {
  val _id = String(Symbol("_id")) 
  val _rev = String(Symbol("_rev"))
}

object Doc extends Doc

object Couch {
  def apply(host: String) = new Http(host, 5984)
  def apply(): Http = Couch("127.0.0.1")
}
  

case class Database(name: String) {
  class H(val http: Http) extends Database(name) {
    def apply(id: String): Http#Request = http("/" + name + "/" + encode(id))
    def all_docs = {
      val Some(rows) = (this("_all_docs") >> { new Store(_) })(Listing.rows)
      for {
        Some(row) <- rows
        id <- (new Store(row))(ListItem.id)
      } yield id
    }
  }
  def apply(http: Http) = new H(http)
    
  object ListItem extends Schema {
    val id = String('id)
  }
  object Listing extends Schema { 
    val rows = List[Map[Symbol, Option[Any]]]('rows)
  }
}

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