import net.databinder.dispatch._

val couch = new Http("localhost", 5984)

object Person extends Doc {
  val name = String('name) 
  val age = Int('age)
  val pets = List[java.lang.String]('pets)
  
  val obj = new Object('objective) {
    val acc = String('accomplished)
  }
}

var p = couch("/people/nathan") >> { new Store(_) }

