import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class CouchSpec extends Spec with ShouldMatchers {
  import dispatch._
  import dispatch.couch._
  import dispatch.json._

  val couch = Couch() // these tests expect CouchDB to be running at 127.0.0.1 on port 5984
  val test = Db("test")
  val empty = Doc(test, "empty")
  val full = Doc(test, "full")
  
  object Test extends Id {
    val content = 'content ? str
  }
  val example = "Ah, you ladies! Always on the spot when there's something happening!"
  
  describe("Database and document create") {
    it("should create a database") {
      couch(test.create)
      couch(test as_str) should startWith("""{"db_name":"test","doc_count":0""")
    }
    it("should create an empty document") {
      couch(empty <<< Js() >|)
      couch(empty ># Id._id) should equal (empty.id) 
    }
    it("should create a document with content") {
      val content = (Test.content << example)(Js())
      couch(full <<< content >|)
      couch(full ># Test.content) should equal (example)
    }
  }
  describe("Document update") {
    it("should update a remote document") {
      val js = couch(empty ># { Test.content << example })
      couch(empty.update(js))
      couch(empty ># Test.content) should equal (example)
    }
    it("should update local revisions to avoid update conflicts") {
      val js = couch(empty ># { Test.content << "1" })
      val update1 = couch(empty.update(js))
      val update2 = couch(empty.update((Test.content << "2")(update1)))
      couch(empty ># Test.content) should equal ("2")
    }
  }
  describe("Document and database delete") {
    import org.apache.http.HttpResponse
    import org.apache.http.HttpEntity
    it("should delete a document") {
      couch(full.delete(couch(full ># Test._rev)))
      couch(
        full { (status: Int, res: HttpResponse, e: Option[HttpEntity]) => status }
      ) should equal (404)
    }

    it("should delete a database") {
      couch(test.delete)
      couch(
        test { (status: Int, res: HttpResponse, e: Option[HttpEntity]) => status }
      ) should equal (404)
    }
  }
}