import org.specs._

class HttpSpec extends Specification {
  import dispatch._
  import Http._
  
  import org.apache.http.protocol.HTTP.CONTENT_ENCODING

  val jane = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.\n"
  
  "Singleton Http test get" should {
    val req: Request = "http://technically.us/test.text"
    "throw exception if credentials are specified without explicit host" in {
      Http (req as ("user", "pass") as_str) must throwAn[Exception]
    }
    get_specs(req)
  }
  "Bound host get" should {
    val req = :/("technically.us") / "test.text"
    "not throw exception if credentials are specified with explicit host" in {
      Http (req as ("user", "pass") as_str) must_== (jane)
    }
    get_specs(req)
  }
  "Combined request get" should {
    get_specs(:/("technically.us") <& /("test.text"))
  }
  "Backwards combined request get" should {
    get_specs(/("test.text") <& :/("technically.us"))
  }
  def get_specs(test: Request) = {
    val http = new Http with Threads
    // start some connections as futures
    val string = http.future_error {
      case e => print(e.getMessage) // compilation test
    } (test.as_str)
    val stream = http.future(test >> { stm => 
      // the nested scenario here contrived fails with actors.Futures
      http.future((test >> { stm =>
        scala.io.Source.fromInputStream(stm).mkString
      }) ~> { string =>
        string // identity function
      })
    })
    val bytes = http.future(test >>> new java.io.ByteArrayOutputStream)
    // test a few other things
    "throw status code exception when applied to non-existent resource" in {
      http (test / "do_not_want" as_str) must throwA[StatusCode]
    }
    "allow any status code with x" in {
      http x (test / "do_not_want" as_str) {
        case (404, _, _, out) => out()
        case _ => "success is failure"
      } must include ("404 Not Found")
    }
    "serve a gzip header" in {
      http(test.gzip >:> { _(CONTENT_ENCODING) }) must_== (Set("gzip"))
    }
    // check back on the futures
    "equal expected string" in {
      string() must_== jane
    }
    "stream to expected sting" in {
      stream()() must_== jane
    }
    "write to expected sting bytes" in {
      bytes().toByteArray.toList must_== jane.getBytes.toList
    }
    
    val h = new Http// single threaded Http instance
    "equal expected string with gzip encoding" in {
      h(test.gzip >+ { r => (r as_str, r >:> { _(CONTENT_ENCODING) }) } ) must_== (jane, Set("gzip"))
    }

    "equal expected string with a gzip defaulter" in {
      val my_defualts = /\.gzip
      h(my_defualts <& test >+ { r => (r as_str, r >:> { _(CONTENT_ENCODING) }) } ) must_== (jane, Set("gzip"))
    }

    "equal expected string without gzip encoding" in {
      h(test >+ { r => (r as_str, r >:> { _(CONTENT_ENCODING) }) }) must_== (jane, Set())
    }
  }
  "Path building responses" should {
    // using singleton Http, will need to shut down after all tests
    val test2 = "and they were both ever sensible of the warmest gratitude\n"
    "work with chaining" in {
      Http( :/("technically.us") / "test" / "test.text" as_str ) must_== test2
    }
    "work with factories" in {
      Http( :/("technically.us") <& /("test") <& /("test.text") as_str ) must_== test2
    }
  }
  doAfterSpec { Http.shutdown  }
}
