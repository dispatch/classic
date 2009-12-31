import org.specs._

class CouchSpec extends Specification {
  import dispatch._
  import dispatch.couch._
  import dispatch.json._
  import JsHttp._

  val http = new Http
  val test = Db(Couch(), "test") // these tests expect CouchDB to be running at 127.0.0.1 on port 5984
  val empty_doc = Doc(test, "empty_doc")
  val full = Doc(test, "full")
  
  object Test extends Id {
    val content = 'content ? str
  }
  val example = "Ah, you ladies! Always on the spot when there's something happening!"
  
  "Database and document create" should {
    "create a database" in {
      http(test.create)
      http(test as_str) must startWith("""{"db_name":"test","doc_count":0""")
    }
    "create an empty_doc document" in {
      http(empty_doc <<< Js() >|)
      http(empty_doc ># Id._id) must_== empty_doc.id
    }
    "create a document with content" in {
      val content = (Test.content << example)(Js())
      http(full <<< content >|)
      http(full ># Test.content) must_== example
    }
    "return new documents from all_docs" in {
      http(test.all_docs) must_== List(empty_doc.id, full.id)
    }
  }
  "Document update" should {
    "update a remote document" in {
      val js = http(empty_doc ># { Test.content << example })
      http(empty_doc.update(js))
      http(empty_doc ># Test.content) must_== example
    }
    "update local revisions to avoid update conflicts" in {
      val js = http(empty_doc ># { Test.content << "1" })
      val update1 = http(empty_doc.update(js))
      val update2 = http(empty_doc.update((Test.content << "2")(update1)))
      http(empty_doc ># Test.content) must_== "2"
    }
  }
  "Document and database delete" should {
    import org.apache.http.HttpResponse
    import org.apache.http.HttpEntity
    "delete a document" in {
      http(full.delete(http(full ># Test._rev)))
      (http when { _ == 404 }) (full ># ('error ! str)) must_== "not_found"
    }
    "delete a database" in {
      http(test.delete)
      // just another way of checking that it returns 404
      (http x test) { (status, _, _) => status } must_== 404
    }
  }
}
