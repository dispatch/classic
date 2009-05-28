import org.scalatest.{Spec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

class HttpSpec extends Spec with ShouldMatchers with BeforeAndAfter {
  import dispatch._
  import Http._

  val jane = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.\n"
  
  describe("Singleton Http test get") {
    val req: Request = "http://technically.us/test.text"
    it("should throw exception if credentials are specified without explicit host") {
      intercept[Exception] {
        Http (req as ("user", "pass") as_str)
      }
    }
    get_specs(req)
  }
  describe("Bound host get") {
    val req = :/("technically.us") / "test.text"
    it("should not throw exception if credentials are specified with explicit host") {
      Http (req as ("user", "pass") as_str) should equal (jane)
    }
    get_specs(req)
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
    
    it("should throw status code exception when applied to non-existent resource") {
      intercept[StatusCode] {
        http (test / "do_not_want" as_str)
      }
    }

    it("should allow any status code with x") {
      http x (test / "do_not_want" as_str) {
        case (404, _, _, out) => out()
        case _ => "success is failure"
      } should include ("404 Not Found")
    }

    it("should equal expected string with gzip encoding") {
      http.also (test.gzip as_str) {
        case (_, _, Some(ent)) if ent.getContentEncoding != null => ent.getContentEncoding.getValue
        case _ => ""
      } should equal (jane, "gzip")
    }

    it("should equal expected string with a gzip defaulter") {
      val my_defualts = /\.gzip
      http(my_defualts <& test as_str {
        case (_, _, Some(ent), out) if ent.getContentEncoding != null => 
          (out(), ent.getContentEncoding.getValue)
        case _ => ("", "")
      } ) should equal (jane, "gzip")
    }

    it("should equal expected string without gzip encoding") {
      http(test.as_str {
        case (_, _, Some(ent), out) =>
          (out(), if (ent.getContentEncoding == null) "" else ent.getContentEncoding.getValue)
        case _ => ("", "")
      }) should equal (jane, "")
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
