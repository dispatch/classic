import net.databinder.dispatch._
import net.databinder.dispatch.couch._
import Js._

val js = Js(""" { "a": {"b": 2} } """)

trait Test extends Js {
  val a = new Obj('a) {
    val b = Num('b)
  }
}
