import java.io.File
import org.eclipse.jetty.server.handler.{DefaultHandler, HandlerList, ResourceHandler}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.specs._
import dispatch._
import dispatch.tagsoup._

object TagSoupSpec extends Specification with ServedByJetty {
  val port = 9990

  object BadHtml_with_ImplicitTagSoupHandlers
    extends Request(:/("localhost", port) / "Human.html")
    with ImplicitTagSoupHandlers

  class BadHtmlClass1(request: Request = (:/("localhost", port) / "Human.html"))
    extends Request(request: Request)
    with ImplicitTagSoupHandlers

  class BadHtmlClass2(request: Request = (:/("localhost", port) / "Human.html"))
    extends Request(request: Request)

  object BadHtml2
    extends Request(:/("localhost", port) / "Human.html")
    with ImplicitTagSoupHandlers

  object BadHtml
    extends Request(:/("localhost", port) / "Human.html")

  "Using </>" should {
    "fail to parse resource" in {
      withResourceServer() { _ =>
        val request = :/("localhost", port) / "Human.html"

        Http(request </> { nodes =>
          (nodes \\ "title").text
        }) must throwA[scala.xml.parsing.FatalError]
      }
    }
  }

  "Extending implicit TagSoup" should {
    "make BadHtml parsable" in {
      withResourceServer() { _ =>
        val request = BadHtml_with_ImplicitTagSoupHandlers
        val title = Http(request tagsouped { nodes =>
            (nodes \\ "title").text
        })
        title must be_==("Human")
      }
    }

    "make BadHtmlClass1 (class extend implicit) parsable, though this is ugly" in {
      withResourceServer() { _ =>
        var request = new BadHtmlClass1()
        val title = Http(request.requestToTagSoupHandlers(request) tagsouped { nodes =>
            (nodes \\ "title").text
        })
        title must be_==("Human")
      }
    }

    "make BadHtmlClass2 (instance extends implicit) parsable, though this is ugly" in {
      withResourceServer() { _ =>
        var request = new BadHtmlClass2() with ImplicitTagSoupHandlers
        val title = Http(request.requestToTagSoupHandlers(request) tagsouped { nodes =>
          (nodes \\ "title").text
        })

        title must be_==("Human")
      }
    }
  }

  "Implicit TagSoupHttp converters in scope" should {
    import TagSoupHttp._
    "make Request parsable" in {
      withResourceServer() { _ =>
        val request = :/("localhost", port) / "Human.html"
        val title = Http(request tagsouped { nodes =>
          (nodes \\ "title").text
        })

        title must be_==("Human")
      }
    }

    "make BadHtmlClass1 (class extend implicit) parsable" in {
      withResourceServer() { _ =>
        var request = new BadHtmlClass1()
        val title = Http(request tagsouped { nodes =>
          (nodes \\ "title").text
        })

        title must be_==("Human")
      }
    }

    "make BadHtmlClass2 (instance extends implicit) parsable" in {
      withResourceServer() { _ =>
        var request = new BadHtmlClass2() with ImplicitTagSoupHandlers
        val title = Http(request tagsouped { nodes =>
          (nodes \\ "title").text
        })

        title must be_==("Human")
      }
    }

    "make BadHtmlClass2 parsable" in {
      withResourceServer() { _ =>
        var request = new BadHtmlClass2()
        val title = Http(request tagsouped { nodes =>
          (nodes \\ "title").text
        })

        title must be_==("Human")
      }
    }

    "make BadHtml (object) parsable" in {
      withResourceServer() { _ =>
        val request = BadHtml
        val title = Http(request tagsouped { nodes =>
          (nodes \\ "title").text
        })

        title must be_==("Human")
      }
    }
  }

  """Using the verb <\\>""" should {
    "do the same thing as the verb tagsouped" in {
      withResourceServer() { _ =>
        val request = BadHtml_with_ImplicitTagSoupHandlers

        val title1 = Http(request <\\> { nodes =>
          (nodes \\ "title").text
        })
        val title2 = Http(request tagsouped { nodes =>
          (nodes \\ "title").text
        })

        title1 must be_==(title2)
      }
    }
  }

  "Using the verb as_tagsouped" should {
    "return the nodes" in {
      withResourceServer() { _ =>
        val request = BadHtml_with_ImplicitTagSoupHandlers
        val ns = Http(request as_tagsouped)

        (ns \\ "title").text must be_==("Human")
      }
    }
  }
}

trait ServedByJetty {
  def withResourceServer(resourceBase: String = "./tagsoup/src/test/resources", port: Int = 9990)(op: Unit => Unit) {
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
      op()
    } finally {
      server.stop
    }
  }
}