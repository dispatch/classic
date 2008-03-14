import net.databinder.http.Http

val couch = new Http("localhost", 5984)

import net.databinder.json._

object Person {
  val name = string('name)
  val obj = new JsObject('objective) {
    val acc = JsString('accomplished)
  }
}

val p = couch("/nathan/7917252010D57024A75A16F20903FB72")(new JsStore(_))