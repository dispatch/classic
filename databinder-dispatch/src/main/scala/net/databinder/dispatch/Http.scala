package net.databinder.dispatch

import java.io.InputStream

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

class Http(host: String, port: int) extends HttpClient {
  getHostConfiguration.setHost(host, port)
  
  def exec [T] (m: HttpMethod) = new {
    def apply(thunk: (Int) => T) = try {
      thunk(executeMethod(m))
    } finally { m.releaseConnection() }

    def when (chk: (Int) => Boolean)(thunk: (HttpMethod) => T) = 
      this { code =>
        if (chk(code))
          thunk(m)
        else
          error("Response not OK: " + code)
      }
    
    def ok (thunk: HttpMethod => T) = 
        (this when { code => code >= 200 && code < 300 })(thunk)
  }
  def apply(uri: String) = new Request(uri)
  
  class Request(uri: String) {
    def >> [T] (thunk: (InputStream) => T) =
      exec(new GetMethod(uri)) ok (m => thunk(m.getResponseBodyAsStream))

    def >> = exec(new GetMethod(uri)) ok (m => m.getResponseBodyAsString)
    
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
      def >> [T] (thunk: (InputStream) => T) = (Http.this exec m) ok (m => thunk(m.getResponseBodyAsStream))
      def as_str = exec(m) ok (m => m.getResponseBodyAsString)
    }
  }

}