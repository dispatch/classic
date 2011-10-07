import org.specs._

object JsValueSpec extends Specification {
  import dispatch.json._
  import java.io.ByteArrayInputStream

  val testFile = getClass.getClassLoader.getResourceAsStream("test.json")
  val json = scala.io.Source.fromInputStream(testFile, "utf-8").mkString

  def time[T](block: => T): (T, Long) = {
    val start = System.currentTimeMillis
    (block, System.currentTimeMillis - start)
  }

  "JsValue.fromString and JsValue.fromStream" should {
    "not become slow under alternate calling" in {
      val (_, t1) = time { JsValue.fromString(json) }
      t1 must be_<=(1000L)  // normally finish within 1s
      val (_, t2) = time { JsValue.fromStream(new ByteArrayInputStream(json.getBytes("utf-8"))) }
      t2 must be_<=(1000L)
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
      }.map{_.get}.toList mustNotExist {case (_, t) => t > 1000}
    }
  }
}
