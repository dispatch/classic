import net.databinder.http.Http

val couch = new Http("localhost", 5984)

import net.databinder.json._

object Person extends Schema {
  val name = String('name) 
  val age = Int('age)
  val pets = List[java.lang.String]('pets)
  
  val obj = new Object('objective) {
    val acc = String('accomplished)
  }
}

val p = couch("/people/nathan")(new Store(_))