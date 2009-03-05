import net.databinder.dispatch._
import net.databinder.dispatch.couch._
import Js._

Database("friday")(Couch("toto")).all_docs

val js = Js(""" { "a": {"b": 2, "c": {"o": "last"} } } """)
/*
object Test extends Js {
  val a = new Obj('a) {
    val b = 'b ! num // a is imilicit param
    val c = new Obj('c) {
      val o = 'o ! str
    }
  }
}
*/