import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class CouchSpec extends Spec with ShouldMatchers {
  import dispatch._
  import dispatch.couch._
  import dispatch.json._

  val http = new Http
  val test = Db(Couch(), "test") // these tests expect CouchDB to be running at 127.0.0.1 on port 5984
  val empty = Doc(test, "empty")
  val full = Doc(test, "full")
  
  object Test extends Id {
    val content = 'content ? str
  }
  val example = "Ah, you ladies! Always on the spot when there's something happening!"
  
  describe("Database and document create") {
    it("should create a database") {
      http(test.create)
      http(test as_str) should startWith("""{"db_name":"test","doc_count":0""")
    }
    it("should create an empty document") {
      http(empty <<< Js() >|)
      http(empty ># Id._id) should equal (empty.id) 
    }
    it("should create a document with content") {
      val content = (Test.content << example)(Js())
      http(full <<< content >|)
      http(full ># Test.content) should equal (example)
    }
  }
  describe("Document update") {
    it("should update a remote document") {
      val js = http(empty ># { Test.content << example })
      http(empty.update(js))
      http(empty ># Test.content) should equal (example)
    }
    it("should update local revisions to avoid update conflicts") {
      val js = http(empty ># { Test.content << "1" })
      val update1 = http(empty.update(js))
      val update2 = http(empty.update((Test.content << "2")(update1)))
      http(empty ># Test.content) should equal ("2")
    }
  }
  describe("Document and database delete") {
    import org.apache.http.HttpResponse
    import org.apache.http.HttpEntity
    it("should delete a document") {
      import Js._
      http(full.delete(http(full ># Test._rev)))
      (http when { _ == 404 }) (full ># ('error ! str)) should equal ("not_found")
    }
    it("should delete a database") {
      http(test.delete)
      // just another way of checking that it returns 404
      (http x test) { (status, _, _) => status } should equal (404)
    }
  }
}
