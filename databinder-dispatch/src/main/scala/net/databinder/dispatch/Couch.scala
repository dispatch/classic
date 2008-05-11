package net.databinder.dispatch

import java.io.InputStream

trait Doc extends Schema {
  val _id = String(Symbol("_id")) 
  val _rev = String(Symbol("_rev"))
}

object Doc extends Doc

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
