import net.databinder.http.Http

val couch = new Http("localhost", 5984)

import net.databinder.json.{Schema,Store}
import net.databinder.couch.Doc

object Person extends Doc {
  val name = String('name) 
  val age = Int('age)
  val pets = List[java.lang.String]('pets)
  
  val obj = new Object('objective) {
    val acc = String('accomplished)
  }
}

var p = couch("/people/nathan") >> { new Store(_) }

import net.databinder.couch._