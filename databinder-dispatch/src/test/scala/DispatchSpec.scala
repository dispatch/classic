import org.scalatest.Spec

class JsonSpec extends Spec {
  import net.databinder.dispatch._
  import Js._

  val js = Js(""" { "a": {"a": "a string", "b": {"pi": 3.14159265 } }, "b": [1,2,3] } """)

  describe("Parsed Json") {
    it("should equal expected map") {
      assert( js === Map(
        'a -> Some(Map(
          'a -> Some("a string"), 
          'b -> Some(Map(
            'pi -> Some(3.14159265)
          ))
        )), 
        'b -> Some(List(Some(1), Some(2), Some(3)))
      ))
    }
  }
  describe("Layered extractor object") {
    object TextExtractor extends Js {
      val a = new Obj('a) {
        val a = 'a ? str
        val b = new Obj('b) {
          val pi = 'pi ? num
        }
      }
      val b = 'b ? list(num)
    }
    it("should match against top level object") {
      val TextExtractor.a(a) = js
      assert( a === js('a).get )
    }
    it("should match against second level string") {
      val TextExtractor.a.a(a) = js
      assert( a === "a string" )
    }
    it("should match against third level number") {
      val TextExtractor.a.b.pi(p) = js
      assert( p === 3.14159265 )
    }
    it("should match against a numeric list") {
      val TextExtractor.b(b) = js
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
      assert( a_ === js('a).get )
    }
    it("should deeply extract a third level number") {
      val a(b(pi(pi_))) = js
      assert( pi_ === 3.14159265 )
    }
    it("should match against an untyped list") {
      val l(l_) = js
      assert( l_ === List(Some(1), Some(2), Some(3)))
    }
  }
  describe("Function extractor") {
    def fun[T](ext: Js#M => T) = ext(js)
    it("should extract a top level object") {
      assert( fun('a ! obj) === js('a).get )
    }
    it("should extract a second level string") {
      assert( fun { ('a ! obj) andThen ('a ! str) } === "a string" )
    }
    it("should extract a third level number") {
      assert( fun { ('a ! obj) andThen ('b ! obj) andThen ('pi ! num) } === 3.14159265 )
    }
    it("should work with map") {
      assert( List(js, js, js).map ('b ! list(num)) === List.tabulate(3, _ => List(1,2,3)) )
    }
  }
}

