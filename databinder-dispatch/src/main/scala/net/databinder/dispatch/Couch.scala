package net.databinder.dispatch

import java.io.InputStream
import net.databinder.http.Http
import net.databinder.json.{Schema,Store}

trait Doc extends Schema {
  val _id = String(new Symbol("_id")) 
  val _rev = String(new Symbol("_rev"))
}

object Doc extends Doc

object Revise extends Schema {
  val id = String('id)
  val rev = String('rev)

  def update(stream: InputStream, source: Store) =
    (source << Doc._rev)(new Store(stream)(rev))
    
  def apply(source: Store) = new {
    def <<: (act: Http#Action) =
      (act << source) >> (update(_, source))
  }
}
