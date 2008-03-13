import net.databinder.http.Http

val couch = new Http("localhost", 5984)

import net.databinder.json.JsSchema

trait Person extends JsSchema {
  def name = string('name)
  def age = number('age)
  def pets = list[Any]('pets)
  def parents = list[String]('parents)
}