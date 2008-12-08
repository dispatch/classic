package net.databinder.dispatch

import java.io.InputStream
import org.apache.http.HttpHost

trait Doc extends Schema {
  val _id = String(Symbol("_id")) 
  val _rev = String(Symbol("_rev"))
}

object Doc extends Doc

class Database(host: HttpHost, name: String) extends HttpServer(host)  {
  def this(hostname: String, port: Int, name: String) = this(new HttpHost(hostname, port), name)
  def this(name: String) = this(new HttpHost("localhost", 5984), name)
  
  override def apply(uri: String) = new Request("/"  + name + "/" + uri)
  def all_docs = (apply("_all_docs") >> { new Store(_) } )(Listing.rows)
  
  object Listing extends Schema { 
    class ListItem(symbol: Symbol) extends Object(symbol) {
      val id = String('id)
    }
    val rows = List[String]('rows)
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
