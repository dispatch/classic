import org.specs._
import dispatch._
import dispatch.tagsoup._

object TagSoupSpec extends Specification {
  object BadHtml_with_ImplicitTagSoupHandlers
    extends Request(:/("oregonstate.edu") / "instruct" / "phl302" / "texts" / "hobbes" / "leviathan-c.html")
    with ImplicitTagSoupHandlers

  object BadHtml
    extends Request(:/("oregonstate.edu") / "instruct" / "phl302" / "texts" / "hobbes" / "leviathan-c.html")

  class BadHtmlClass1(request: Request = (:/("oregonstate.edu") / "instruct" / "phl302" / "texts" / "hobbes" / "leviathan-c.html"))
    extends Request(request: Request)
    with ImplicitTagSoupHandlers

  class BadHtmlClass2(request: Request = (:/("oregonstate.edu") / "instruct" / "phl302" / "texts" / "hobbes" / "leviathan-c.html"))
    extends Request(request: Request)

  "Extending implicit TagSoup" should {
    "make BadHtml parsable" in {
      val request = BadHtml_with_ImplicitTagSoupHandlers
      val title = Http(request tagsouped { nodes =>
        (nodes \\ "title").text
      })

      title must be_==("The Leviathan by Thomas Hobbes")
    }

    "make BadHtmlClass1 (class extend implicit) parsable, though this is ugly" in {
      var request = new BadHtmlClass1()
      val title = Http(request.requestToTagSoupHandlers(request) tagsouped { nodes =>
//      val title = Http(request tagsouped { nodes =>
        (nodes \\ "title").text
      })

      title must be_==("The Leviathan by Thomas Hobbes")
    }

    "make BadHtmlClass2 (instance extends implicit) parsable, though this is ugly" in {
      var request = new BadHtmlClass2() with ImplicitTagSoupHandlers
      val title = Http(request.requestToTagSoupHandlers(request) tagsouped { nodes =>
        (nodes \\ "title").text
      })

      title must be_==("The Leviathan by Thomas Hobbes")
    }
  }

  "Implicit TagSoupHttp converters in scope" should {
    import TagSoupHttp._
    "make Request parsable" in {
      val request = :/("oregonstate.edu") / "instruct" / "phl302" / "texts" / "hobbes" / "leviathan-c.html"
      val title = Http(request tagsouped { nodes =>
        (nodes \\ "title").text
      })

      title must be_==("The Leviathan by Thomas Hobbes")
    }

    "make BadHtmlClass1 (class extend implicit) parsable" in {
      var request = new BadHtmlClass1()
      val title = Http(request tagsouped { nodes =>
        (nodes \\ "title").text
      })

      title must be_==("The Leviathan by Thomas Hobbes")
    }

    "make BadHtmlClass2 (instance extends implicit) parsable" in {
      var request = new BadHtmlClass2() with ImplicitTagSoupHandlers
      val title = Http(request tagsouped { nodes =>
        (nodes \\ "title").text
      })

      title must be_==("The Leviathan by Thomas Hobbes")
    }

    "make BadHtmlClass2 parsable" in {
      var request = new BadHtmlClass2()
      val title = Http(request tagsouped { nodes =>
        (nodes \\ "title").text
      })

      title must be_==("The Leviathan by Thomas Hobbes")
    }

    "make BadHtml (object) parsable" in {
      val request = BadHtml
      val title = Http(request tagsouped { nodes =>
        (nodes \\ "title").text
      })

      title must be_==("The Leviathan by Thomas Hobbes")
    }
  }

  """Using the verb <\\>""" should {
    "do the same thing as the verb tagsouped" in {
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

  "Using the verb as_tagsouped" should {
    "return the nodes" in {
      val request = BadHtml_with_ImplicitTagSoupHandlers
      val ns = Http(request as_tagsouped)

      (ns \\ "title").text must be_==("The Leviathan by Thomas Hobbes")
    }
  }
}