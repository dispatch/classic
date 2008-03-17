package net.databinder.dispatch

import java.io.InputStream

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

class Http(host: String, port: int) extends HttpClient {
  getHostConfiguration.setHost(host, port)
  
  def exec [T] (m: HttpMethod)(thunk: (Int) => T) = try {
    thunk(executeMethod(m))
  } finally { m.releaseConnection() }

  def exec_if [T] (m: HttpMethod)(chk: (Int) => Boolean)(ok: (HttpMethod) => T) = 
    (this exec m) { code =>
      if (chk(code))
        ok(m)
      else
        error("Response not OK: " + code) }
  
  def exec_if_ok [T] (m: HttpMethod)(ok: (HttpMethod) => T) = 
    (this exec_if m){ code => code >= 200 && code < 300 }(ok)
  
  def apply(uri: String) = new Request(uri)
  
  class Request(uri: String) {
    def >> [T] (ok: (InputStream) => T) =
      (Http.this exec_if_ok new GetMethod(uri))(m => ok(m.getResponseBodyAsStream))

    def >> = (Http.this exec_if_ok new GetMethod(uri))(m => m.getResponseBodyAsString)
    
    def << [T] (body: T) = {
      val m = new PutMethod(uri)
      m setRequestEntity new StringRequestEntity(body.toString)
      new Response(m)
    }
    
    def << [T] (values: Tuple2[String, Any]*) = {
      val m = new PostMethod(uri)
      values foreach { tup => m.setParameter(tup._1, tup._2.toString) }
      m.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
      new Response(m)
    }
    class Response(m: HttpMethod) {
      def >> [T] (ok: (InputStream) => T) = (Http.this exec_if_ok m)(m => ok(m.getResponseBodyAsStream))
      def as_str = (Http.this exec_if_ok m)(m => m.getResponseBodyAsString)
    }
  }

}