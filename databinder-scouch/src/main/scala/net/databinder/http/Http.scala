package net.databinder.http

import java.io.InputStream

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

class Http(host: String, port: int) extends HttpClient {
  getHostConfiguration.setHost(host, port)
  
  def ? [T] (m: HttpMethod)(thunk: (Int) => T) = try {
    thunk(executeMethod(m))
  } finally { m.releaseConnection() }
  
  def ?! [T] (m: HttpMethod)(check: (Int) => Boolean)(okay: (InputStream) => T) = 
    (this ? m) { code =>
      if (check(code))
        okay(m.getResponseBodyAsStream())
      else
        error("Response not OK: " + code) }
  
  def ! [T] (m: HttpMethod)(okay: (InputStream) => T) = 
    (this ?! m){ code => code >= 200 && code < 300 }(okay)
  
  def apply(uri: String) = new Action(uri)
  
  class Action(uri: String) {
    def >> [T] (okay: (InputStream) => T) = 
      (Http.this ! new GetMethod(uri))(okay)

    def << [T] (body: T) = new {
      def >> (okay: (InputStream) => T) = {
        val m = new PutMethod(uri)
        m setRequestEntity new StringRequestEntity(body.toString)
        (Http.this ! m)(okay)
      }
    }
    
  }
}