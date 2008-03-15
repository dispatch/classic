import net.databinder.http.Http

val couch = new Http("localhost", 5984)

import net.databinder.json._

object Person extends JsObject {
  val name = JsString('name) 
  
  val obj = new JsObject('objective) {
    val acc = JsString('accomplished)
  }
}

val p = couch("/people/nathan")(new JsStore(_))