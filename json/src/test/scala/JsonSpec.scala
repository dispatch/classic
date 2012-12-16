import org.specs._

object JsValueSpec extends Specification {
  import dispatch.classic.json._
  import java.io.ByteArrayInputStream

  val testFile = getClass.getClassLoader.getResourceAsStream("test.json")
  val json = scala.io.Source.fromInputStream(testFile, "utf-8").mkString

  def time[T](block: => T): (T, Long) = {
    val start = System.currentTimeMillis
    (block, System.currentTimeMillis - start)
  }

  def reparse(x: Any) = Js(JsValue(x).toString)

  "JS parsing" should {
    "parse null" in {
      Js("null") must be equalTo(JsNull)
    }
    "parse true" in {
      Js("true") must be equalTo(JsTrue)
    }
    "parse false" in {
      Js("false") must be equalTo(JsFalse)
    }
    "parse number" in {
      Js("2394.3") must be equalTo(JsNumber(BigDecimal(2394.3)))
    }
    "parse string" in {
      Js("\"foobie bletch\"") must be equalTo(JsString("foobie bletch"))
    }
    "parse escaped string" in {
      Js("\"hello\\u203D\"") must be equalTo(JsString("hello\u203d"))
    }
    "parse array" in {
      Js("[\"foo\", \"bar\"]") must be equalTo(JsArray(List(JsString("foo"), JsString("bar"))))
    }
    "parse object" in {
      Js("{\"foo\": true}") must be equalTo(JsObject(Map(JsString("foo") -> JsTrue)))
    }
  }
  
  "JS round-tripping" should {
    "round-trip null" in {
      reparse(null) must be equalTo(JsNull)
    }
    "round-trip string" in {
      reparse("HACKEM MUCHE") must be equalTo(JsString("HACKEM MUCHE"))
    }
    "round-trip array" in {
      reparse(List("foo", false)) must be equalTo(JsArray(List(JsString("foo"), JsFalse)))
    }
    "round-trip object" in {
      reparse(Map("scrolls" -> List("identify"), "name" -> "foo")) must be equalTo(
        JsObject(Map(JsString("scrolls") -> JsArray(List(JsString("identify"))),
                     JsString("name") -> JsString("foo"))))
    }
    "round-trip Unicode string" in {
      reparse("control \u0008") must be equalTo(JsString("control \u0008"))
    }
  }

  "JsValue.fromString and JsValue.fromStream" should {
    val maxTime = 1000L
    "not become slow under alternate calling" in {
      val (_, t1) = time { JsValue.fromString(json) }
      t1 must be_<=(maxTime)  // normally finish within 1s
      val (_, t2) = time { JsValue.fromStream(new ByteArrayInputStream(json.getBytes("utf-8"))) }
      t2 must be_<=(maxTime)
    }
    
    "work properly under multi-thread calling" in {
      import java.util.concurrent._
      val numOfThreads = 20
      val executor = Executors.newFixedThreadPool(numOfThreads)
      (1 to numOfThreads).map{i => 
        val f = new FutureTask(new Callable[Pair[JsValue, Long]]{
          def call =
            if (i % 2 == 0) time { JsValue.fromString(json) }
            else time { JsValue.fromStream(new ByteArrayInputStream(json.getBytes("utf-8"))) }
          })
        executor.execute(f)
        f
      }.map{_.get}.toList mustNotExist {case (_, t) => t > maxTime}
    }
  }
}
