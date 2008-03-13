package net.databinder.http

import java.io.InputStream

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

class Http(host: String, port: int) extends HttpClient {
  getHostConfiguration.setHost(host, port)
  
  def ? [T] (m: HttpMethod)(okay: (InputStream) => T)(fail: (Int, HttpMethod)=>T) = 
    try { executeMethod(m) match {
        case 200 => okay(m.getResponseBodyAsStream())
        case code => fail(code, m)
      } 
    } finally { m.releaseConnection() }
  
  def ! [T] (m: HttpMethod)(okay: (InputStream) => T) = 
    (this ? m)(okay){ (code, method) => error("Response not OK: " + code) }
  
  def apply[T](uri: String)(okay: (InputStream) => T) = (this ! new GetMethod(uri))(okay)
}