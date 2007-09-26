import scala.util.parsing.json._

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

val cl = new HttpClient
cl.getHostConfiguration.setHost("localhost", 8888)
def ex(m: HttpMethod) = { println(cl executeMethod m); m getResponseBodyAsString }


