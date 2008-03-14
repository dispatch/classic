import net.databinder.http.Http

val couch = new Http("localhost", 5984)

import net.databinder.json._

object Person extends JsObject {
  val name = my('name) as new JsString
  val obj = my('objective) as new JsObject {
    val acc = my('accomplished) as new JsString
  }
}

val p = couch("/people/nathan")(new JsStore(_))