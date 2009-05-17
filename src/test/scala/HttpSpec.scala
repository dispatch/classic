import org.scalatest.{Spec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

class HttpSpec extends Spec with ShouldMatchers with BeforeAndAfter {
  import dispatch._
  import Http._

  val jane = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.\n"
  
  describe("Singleton Http test get") {
    get_specs("http://technically.us/test.text")
  }
  describe("Bound host get") {
    get_specs(:/("technically.us") / "test.text")
  }
  describe("Combined request get") {
    get_specs(:/("technically.us") <& /("test.text"))
  }
  describe("Backwards combined request get") {
    get_specs(/("test.text") <& :/("technically.us"))
  }
  def get_specs(test: Request) = {
    val http = new Http // using a single-threaded Http instance
    it("should equal expected string") {
      http(test.as_str) should equal (jane)
    }
    it("should stream to expected sting") {
      http(test >> { stm => io.Source.fromInputStream(stm).mkString }) should equal (jane)
    }
    it("should write to expected sting bytes") {
      http(test >>> new java.io.ByteArrayOutputStream).toByteArray should equal (jane.getBytes)
    }

    it("should equal expected string with gzip encoding") {
      http.also (test.gzip as_str) {
        case (_, _, Some(ent)) if ent.getContentEncoding != null => ent.getContentEncoding.getValue
        case _ => ""
      } should equal (jane, "gzip")
    }

    it("should equal expected string with a gzip defaulter") {
      val my_defualts = /\.gzip
      http.also (my_defualts <& test as_str) {
        case (_, _, Some(ent)) if ent.getContentEncoding != null => ent.getContentEncoding.getValue
        case _ => ""
      } should equal (jane, "gzip")
    }

    it("should equal expected string without gzip encoding") {
      http.also (test as_str) {
        case (_, _, Some(ent)) if ent.getContentEncoding != null => ent.getContentEncoding.getValue
        case _ => ""
      } should equal (jane, "")
    }
    it("should equal expected string without gzip encoding") {
      http (test as_str {
        case (_, _, Some(ent), as_str) if ent.getContentEncoding != null => 
          (as_str, ent.getContentEncoding.getValue)
        case _ => ("", "")
      } ) should equal (jane, "")
    }
  }
  describe("Path building responses") {
    // using singleton Http, will need to shut down after all tests
    val test2 = "and they were both ever sensible of the warmest gratitude\n"
    it("should work with chaining") {
      Http( :/("technically.us") / "test" / "test.text" as_str ) should equal (test2)
    }
    it("should work with factories") {
      Http( :/("technically.us") <& /("test") <& /("test.text") as_str ) should equal (test2)
    }
  }
  override def afterAll {
    Http.shutdown
  }
}
