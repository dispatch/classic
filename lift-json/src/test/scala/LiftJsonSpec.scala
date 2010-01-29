import org.specs._

object LiftJsonSpec extends Specification {
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._

  val test = :/("technically.us") / "test.json"
  
  "Json Parsing" should {
    "find title of test glossary" in {
      val http = new Http
      http(test ># { js =>
        for (JString(s) <- js \ "glossary" \ "title") yield s
      } ) must_== List("example glossary")
    }
    "find reference list" in {
      val http = new Http
      http(test ># { js =>
        for (JField("GlossSeeAlso", JArray(l)) <- js; JString(s) <- l) yield s
      } ) must_== List("GML","XML")
    }
  }
}
