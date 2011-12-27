import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.{ResponseHandler, HttpClient}
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, HttpHost}
import org.specs._

object HttpSpec extends Specification {
  import dispatch._
  
  import org.apache.http.protocol.HTTP.CONTENT_ENCODING

  val jane = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.\n"
  
  "Singleton Http test get" should {
    val req = new Request("http://technically.us/test.text")
    "not throw exception if credentials are specified without explicit host" in {
      Http (req as ("user", "pass") as_str) must_== (jane)
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

  "Http" should {
    class SimpleDelegatingHttpClient(realClient: HttpClient) extends HttpClient {
      def getParams = realClient.getParams
      def getConnectionManager = realClient.getConnectionManager
      def execute(request: HttpUriRequest) = realClient.execute(request)
      def execute(request: HttpUriRequest, context: HttpContext) = realClient.execute(request, context)
      def execute(target: HttpHost, request: HttpRequest) = realClient.execute(target, request)
      def execute(target: HttpHost, request: HttpRequest, context: HttpContext) = realClient.execute(target, request, context)
      def execute[T](request: HttpUriRequest, responseHandler: ResponseHandler[_ <: T]) = realClient.execute(request, responseHandler)
      def execute[T](request: HttpUriRequest, responseHandler: ResponseHandler[_ <: T], context: HttpContext) = realClient.execute(request, responseHandler, context)
      def execute[T](target: HttpHost, request: HttpRequest, responseHandler: ResponseHandler[_ <: T]) = realClient.execute(target, request, responseHandler)
      def execute[T](target: HttpHost, request: HttpRequest, responseHandler: ResponseHandler[_ <: T], context: HttpContext) = realClient.execute(target, request, responseHandler, context)
    }

    "allow override" in {
      val http = new Http with thread.Safety {
        override def make_client: HttpClient = new SimpleDelegatingHttpClient(super.make_client)
      }
      http must notBeNull // i.e. this code should compile
      http.shutdown()
    }
  }

  val http = new Http
  val httpfuture = new thread.Http
  def get_specs(test: Request) = {
    // start some connections as futures
    val stream = httpfuture(test >> { stm => 
      // the nested scenario here contrived fails with actors.Futures
      httpfuture((test >> { stm =>
        scala.io.Source.fromInputStream(stm).mkString
      }) ~> { string =>
        string // identity function
      })
    })
    val string = httpfuture(test as_str)
    val bytes = httpfuture(test >>> new java.io.ByteArrayOutputStream)
    // test a few other things
    "throw status code exception when applied to non-existent resource" in {
      http (test / "do_not_want" as_str) must throwA[StatusCode]
    }
    "allow any status code with x" in {
      (http x (test / "do_not_want" as_str) {
        case (404, _, _, out) => out()
        case _ => "success is failure"
      }) must include ("404 Not Found")
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
    
    "equal expected string with gzip encoding, using future" in {
      httpfuture(test.gzip >+ { r => (r as_str, r >:> { _(CONTENT_ENCODING) }) } )() must_== (jane, Set("gzip"))
    }
    val h = new Http// single threaded Http instance
    "equal expected string with a gzip defaulter" in {
      val my_defaults = /\.gzip
      h(my_defaults <& test >+ { r => (r as_str, r >:> { _(CONTENT_ENCODING) }) } ) must_== (jane, Set("gzip"))
    }

    "process html page" in {
      import XhtmlParsing._
      h(url("http://technically.us/") </> { xml =>
        (xml \\ "title").text
      }) must_== "technically.us"
    }

    "process xml response" in {
      h(url("http://technically.us/test.xml") <> { xml =>
        (xml \ "quote").text.trim
      }) must_== jane.trim
    }

    "equal expected string without gzip encoding, with handler chaining" in {
      h(test >+> { r => r >:> { headers =>
        r >- { (_, headers(CONTENT_ENCODING)) }
      } }) must_== (jane, Set())
    }
    "equal expected string with gzip encoding, with >:+" in {
      h(test.gzip >:+ { (headers, r) =>
        r >- { (_, headers(CONTENT_ENCODING.toLowerCase)) }
      }) must_== (jane, Seq("gzip"))
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
  doAfterSpec {
    Http.shutdown()
    http.shutdown()
    httpfuture.shutdown()
  }
}
