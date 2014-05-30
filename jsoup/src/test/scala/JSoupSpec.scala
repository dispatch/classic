import org.eclipse.jetty.server.handler.{DefaultHandler, HandlerList, ResourceHandler}
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.Server
import org.specs2.mutable.Specification
import dispatch.classic._

object JSoupSpec extends Specification with ServedByJetty {
  val port = 9990
  val resourceBase = "./jsoup/src/test/resources"

  object JSoupHtml extends Request(:/("localhost", port))

  "JSoup html parser" should {
    import dispatch.classic.jsoup.JSoupHttp._
    "successfully parse good html" in {
      withResourceServer { _ =>
        val request = JSoupHtml / "test.html"
        Http(request as_jsouped) must not (throwA[Exception])
      }
    }
    "successfully parse bad html" in {
      withResourceServer { _ =>
        val request = JSoupHtml / "Human.html"
        Http(request as_jsouped) must not (throwA[Exception])
      }
    }
  }

  "Using JSoup Document" should {
    import dispatch.classic.jsoup.JSoupHttp._
    "find Elements by tag" in {
      withResourceServer { _ =>
        val request = JSoupHtml / "test.html"
        val elements = Http(request jsouped { doc =>
          doc.getElementsByTag("title");
        })

        elements.size must be_==(1)
        elements.get(0).text must be_==("ApA")
      }
    }

    "make Elements processable as scala collections" in {
      withResourceServer { _ =>
        val request = JSoupHtml / "Human.html"
        val elements = Http(request jsouped { doc =>
          doc.getElementsByTag("h1");
        })

        import scala.collection.JavaConversions._
        val hOnes = elements.iterator.toList.map(e => e.text.reverse)
        hOnes.head must be_==("enivid evigrof ot ,namuh si rre oT")
      }
    }

    "make resolved links absolute" in {
      withResourceServer { _ =>
        import scala.collection.JavaConversions._
        val request = JSoupHtml / "test.html"
        val links = Http(request jsouped { doc =>
          doc.select("a[href]").toList;
        })

        links.head.attr("href")  must be_==("/Human.html")
        links.head.attr("abs:href")  must be_==("http://localhost:9990/Human.html")
      }
    }
  }

  """Using the verb </>""" should {
    import dispatch.classic.jsoup.JSoupHttp._
    "do the same thing as the verb jsouped" in {
      withResourceServer { _ =>
        val request = JSoupHtml / "test.html"

        val title1 = Http(request </> {doc =>
          doc.title
        })
        val title2 = Http(request jsouped {doc =>
          doc.title
        })

        title1 must be_==(title2)
      }
    }
  }

  "Using the verb as_jsoupedNodeSeq" should {
    import dispatch.classic.jsoup.JSoupHttp._
    "use JSoup to retrieve a NodeSeq" in {
      withResourceServer { _ =>
        val request = JSoupHtml / "Human.html"

        val ns = Http(request as_jsoupedNodeSeq)

        (ns \\ "title").text must be_==("Human")
      }
    }
  }
}

trait ServedByJetty {
  val port: Int
  val resourceBase: String

  def withResourceServer[A](op: Unit => A): A = {
    // Configure Jetty server
    val connector = new SelectChannelConnector
    connector.setHost("localhost")
    connector.setPort(port)

    val handler = new ResourceHandler
    handler.setDirectoriesListed(true)
    handler.setResourceBase(resourceBase)
    val handlers = new HandlerList
    handlers.setHandlers(Array(handler, new DefaultHandler))

    val server = new Server
    server.addConnector(connector)
    server.setHandler(handlers)

    // Run server for test and then stop
    try {
      server.start
      op(())
    } finally {
      server.stop
    }
  }
}
