import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class LiftJsonSpec extends Spec with ShouldMatchers {
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._

  val test = :/("technically.us") / "test.json"
  
  describe("Json Parsing") {
    it("should find title of test glossary") {
      val http = new Http
      http(test ># { js =>
        for (JString(s) <- js \ "glossary" \ "title") yield s
      } ) should equal (List("example glossary"))
    }
    it("should find reference list") {
      val http = new Http
      http(test ># { js =>
        for (JField("GlossSeeAlso", JArray(l)) <- js; JString(s) <- l) yield s
      } ) should equal (List("GML","XML"))
    }
  }
}
