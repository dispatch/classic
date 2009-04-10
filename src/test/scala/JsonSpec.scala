import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class JsonSpec extends Spec with ShouldMatchers {
  import dispatch.json._
  import Js._

  val js = Js(""" { "a": {"a": "a string", "b": {"pi": 3.14159265 } }, "b": [1,2,3] } """)
  val expected_map = Map(
    JsString('a) -> JsObject(Map(
      JsString('a) -> JsString("a string"),
      JsString('b) -> JsObject(Map(
        JsString('pi) -> JsNumber(BigDecimal("3.14159265")))
      )
    )),
    JsString('b) -> JsArray(List(JsNumber(1), JsNumber(2), JsNumber(3)))
  )
  val js_list = Js("[1,2,3]")
  val expected_list = List(JsNumber(1), JsNumber(2), JsNumber(3))
  
  describe("Parsed Json") {
    it("should equal expected map") {
      js.self should equal (expected_map)
    }
    it("should equal expected list") {
      js_list.self should equal (expected_list)
    }
    it("should equal itself serilized and reparsed") {
      js should equal (JsValue.fromString(JsValue.toJson(js)))
    }
  }
  describe("Layered extractor object") {
    object TestExtractor extends Js {
      val a = new Obj('a) {
        val a = ('a ? str)
        val b = new Obj('b) {
          val pi = 'pi ? num
        }
      }
      val b = 'b ? (list ! num)
    }
    it("should match against top level object") {
      val TestExtractor.a(a) = js
      a should equal (expected_map(JsString('a)))
    }
    it("should match against second level string") {
      val TestExtractor.a.a(a) = js
      a should equal ("a string")
    }
    it("should match against third level number") {
      val TestExtractor.a.b.pi(p) = js
      p should equal (3.14159265)
    }
    it("should match against a numeric list") {
      val TestExtractor.b(b) = js
      b should equal (List(1,2,3))
    }
  }
  describe("Flat extractors") {
    val a = 'a ? obj
    val b = 'b ? obj
    val pi = 'pi ? num
    val l = 'b ? list
    it("should extract a top level object") {
      val a(a_) = js
      a_ should equal (expected_map(JsString('a)))
    }
    it("should deeply extract a third level number") {
      val a(b(pi(pi_))) = js
      pi_ should equal (3.14159265)
    }
    it("should match against an unextracted list") {
      val l(l_) = js
      l_ should equal (List(JsValue(1), JsValue(2), JsValue(3)))
    }
    val num_list = list ! num
    it("should match for an unenclosed Json list") {
      val num_list(l_) = js_list
      l_ should equal (List(1,2,3))
    }
    it("should pattern-match correct elements") {
      (js match {
        case b(b_) => b_
        case a(a_) => a_
      }) should equal (expected_map(JsString('a)))
    }
  }
  describe("Function extractor") {
    /** to resemble an Http#Response */
    object res { def $ [T](ext: JsF[T]) = ext(js) }
    it("should extract a top level object") {
      res $ ('a ! obj) should equal (expected_map(JsString('a)))
    }
    it("should extract a tuple of top level objects") {
      res $ %('a ! obj, 'b ! list, 'b ! list) should 
        equal (expected_map(JsString('a)), expected_list, expected_list)
    }
    it("should extract a second level string") {
      res $ { ('a ! obj) andThen ('a ! str) } should equal ("a string")
    }
    it("should extract a third level number") {
      res $ { ('a ! obj) andThen ('b ! obj) andThen ('pi ! num) } should equal (3.14159265)
    }
    it("should work with map") {
      List(js, js, js).map ('b ! (list ! num)) should equal (List.tabulate(3, _ => List(1,2,3)))
    }
    def fun_l[T](ext: JsF[T]) = ext(js_list)
    it("should extract unenclosed Json list") {
      fun_l(list ! num) should equal (List(1,2,3))
    }
  }
}

