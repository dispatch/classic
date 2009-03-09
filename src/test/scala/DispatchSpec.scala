import org.scalatest.Spec

class JsonSpec extends Spec {
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
      assert( js.self === expected_map )
    }
    it("should equal expected list") {
      assert( js_list.self === expected_list )
    }
    it("should equal itself serilized and reparsed") {
      assert(js === JsValue.fromString(JsValue.toJson(js)))
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
      assert( a === expected_map(JsString('a)) )
    }
    it("should match against second level string") {
      val TestExtractor.a.a(a) = js
      assert( a === "a string" )
    }
    it("should match against third level number") {
      val TestExtractor.a.b.pi(p) = js
      assert( p === 3.14159265 )
    }
    it("should match against a numeric list") {
      val TestExtractor.b(b) = js
      assert( b === List(1,2,3) )
    }
  }
  describe("Flat extractors") {
    val a = 'a ? obj
    val b = 'b ? obj
    val pi = 'pi ? num
    val l = 'b ? list
    it("should extract a top level object") {
      val a(a_) = js
      assert( a_ === expected_map(JsString('a)) )
    }
    it("should deeply extract a third level number") {
      val a(b(pi(pi_))) = js
      assert( pi_ === 3.14159265 )
    }
    it("should match against an unextracted list") {
      val l(l_) = js
      assert( l_ === List(JsValue(1), JsValue(2), JsValue(3)))
    }
    val num_list = list ! num
    it("should match for an unenclosed Json list") {
      val num_list(l_) = js_list
      assert(l_ === List(1,2,3))
    }
  }
  describe("Function extractor") {
    def fun[T](ext: JsValue => T) = ext(js)
    it("should extract a top level object") {
      assert( fun('a ! obj) === expected_map(JsString('a)) )
    }
    it("should extract a second level string") {
      assert( fun { ('a ! obj) andThen ('a ! str) } === "a string" )
    }
    it("should extract a third level number") {
      assert( fun { ('a ! obj) andThen ('b ! obj) andThen ('pi ! num) } === 3.14159265 )
    }
    it("should work with map") {
      assert( List(js, js, js).map ('b ! (list ! num)) === List.tabulate(3, _ => List(1,2,3)) )
    }
    def fun_l[T](ext: JsValue => T) = ext(js_list)
    it("should extract some(?) unenclosed Json list") {
      assert( fun_l(list ! num) === Some(List(1,2,3)) )
    }
  }
}

