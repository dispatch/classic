import org.specs2.mutable.Specification

object JsonSpec extends Specification {
  import dispatch.classic.json._
  import JsHttp._

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
  
  /** mock ># of Http#Response */
  object res { def ># [T](f: JsF[T]) = f(js) }
  
  "Parsed Json" should {
    "equal expected map" in {
      js.self must_== expected_map
    }
    "equal expected list" in {
      js_list.self must_== expected_list
    }
    "equal itself serilized and reparsed" in {
      js must_== JsValue.fromString(JsValue.toJson(js))
    }
  }
  "Nested extractor object" should {
    object TestExtractor extends Js {
      val a = new Obj('a) {
        val a = ('a ? str)
        val b = new Obj('b) {
          val pi = 'pi ? num
        }
      }
      val b = 'b ? (list ! num)
    }
    "match against top level object" in {
      val TestExtractor.a(a) = js
      a must_== expected_map(JsString('a))
    }
    "match against second level string" in {
      val TestExtractor.a.a(a) = js
      a must_== "a string"
    }
    "match against third level number" in {
      val TestExtractor.a.b.pi(p) = js
      p.toString must_== "3.14159265"
    }
    "match against a numeric list" in {
      val TestExtractor.b(b) = js
      b must_== List(1,2,3)
    }
    "replace second level string" in {
      res ># (TestExtractor.a.a << "land, ho") must_== (Js(
        """ { "a": {"a": "land, ho", "b": {"pi": 3.14159265 } }, "b": [1,2,3] } """
      ))
    }
  }
  "Flat extractors" should {
    val a = 'a ? obj
    val aa = 'a ? str
    val b = 'b ? obj
    val pi = 'pi ? num
    val l = 'b ? list
    "extract a top level object" in {
      val a(a0) = js
      a0 must_== expected_map(JsString('a))
    }
    "deeply extract a third level number" in {
      val a(b(pi(pi0))) = js
      pi0.toString must_== "3.14159265"
    }
    "match against an unextracted list" in {
      val l(l0) = js
      l0 must_== List(JsValue(1), JsValue(2), JsValue(3))
    }
    val num_list = list ! num
    "match for an unenclosed Json list" in {
      val num_list(l0) = js_list
      l0 must_== List(1,2,3)
    }
    "pattern-match correct elements" in {
      (js match {
        case b(b0) => b0
        case a(a0) => a0
      }) must_== expected_map(JsString('a))
    }
    "awkwardly replace second level string" in {
      val a(a0) = js
      res ># (a << (aa << "barnacles, ahoy")(a0)) must_== (Js(
        """ { "a": {"a": "barnacles, ahoy", "b": {"pi": 3.14159265 } }, "b": [1,2,3] } """
      ))
    }
  }
  "Function extractor" should {
    "extract a top level object" in {
      res ># ('a ! obj) must_== expected_map(JsString('a))
    }
    "extract a tuple of top level objects" in {
      res ># %('a ! obj, 'b ! list, 'b ! list) must_==
       (expected_map(JsString('a)), expected_list, expected_list)
    }
    "extract a second level string" in {
      res ># { ('a ! obj) andThen ('a ! str) } must_== "a string"
    }
    "extract a third level number" in {
      (res ># { ('a ! obj) andThen ('b ! obj) andThen ('pi ! num) }).toString must_== "3.14159265"
    }
    "work with map" in {
      List(js, js, js).map ('b ! (list ! num)) must_== (1 to 3).map{ _ => List(1,2,3) }.toList
    }
    def fun_l[T](ext: JsF[T]) = ext(js_list)
    "extract unenclosed Json list" in {
      fun_l(list ! num) must_== List(1,2,3)
    }
  }
  "assertion inserting" should {
    "replace second level string" in {
      res ># ('a << ('a << "barnacles, ahoy")) must_== (Js(
        """ { "a": {"a": "barnacles, ahoy", "b": {"pi": 3.14159265 } }, "b": [1,2,3] } """
      ))
    }
    "replace a second level object with a string" in {
      res ># ('a << ('b << "bonzai!")) must_== (Js(
        """ { "a": {"a": "a string", "b": "bonzai!" } , "b": [1,2,3] } """
      ))
    }
  }
}

