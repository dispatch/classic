package net.databinder.dispatch

import java.io.{InputStream,OutputStream,BufferedInputStream,BufferedOutputStream}

import org.apache.http._
import org.apache.http.client._
import org.apache.http.client.methods._

import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.{HTTP, BasicHttpContext}
import org.apache.http.params.{HttpProtocolParams, BasicHttpParams}
import org.apache.http.util.EntityUtils

class Http(host: Option[HttpHost]) extends org.apache.http.impl.client.DefaultHttpClient {
  def this() = this(None)
  def this(host: HttpHost) = this(Some(host))
  def this(hostname: String) = this(new HttpHost(hostname))
  def this(hostname: String, port: Int) = this(new HttpHost(hostname, port))
  
  override def createHttpParams = {
    val params = new BasicHttpParams
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
    HttpProtocolParams.setContentCharset(params, HTTP.UTF_8)
    HttpProtocolParams.setUseExpectContinue(params, false)
    params
  }
  
  def x [T](req: HttpUriRequest) = new {
    def apply(thunk: (Int, HttpResponse, HttpEntity) => T) = {
      val res = host match {
        case None => execute(req)
        case Some(host) => execute(host, req)
      }
      res.getEntity match {
        case null => error("no response message")
        case ent => try { 
            thunk(res.getStatusLine.getStatusCode, res, ent)
          } finally { ent.consumeContent() }
      }
    }
    
    def when(chk: Int => Boolean)(thunk: HttpEntity => T) = this { (code, res, ent) => 
      if (chk(code))
        thunk(ent)
      else 
        error("Response not OK: " + code)
    }
    
    def ok = (this when {code => (200 to 204) contains code}) _
  }
  def apply(uri: String) = new Request(uri)
  
  class Request(uri: String) extends Respond(new HttpGet(uri)) {
    def << (body: Any) = {
      val m = new HttpPut(uri)
      m setEntity new StringEntity(body.toString, HTTP.UTF_8)
      HttpProtocolParams.setUseExpectContinue(m.getParams, false)
      new Respond(m)
    }
    def << (values: (String, Any)*) = {
      val m = new HttpPost(uri)
      m setEntity new UrlEncodedFormEntity(
        (values map) { tup => new BasicNameValuePair(tup._1, tup._2.toString) }.toArray,
        HTTP.UTF_8
      )
      new Respond(m)
    }
  }
  class Respond(req: HttpUriRequest) {
    def >> [T] (thunk: InputStream => T) = x (req) ok (res => thunk(res.getContent))
    def as_str = x (req) ok { EntityUtils.toString(_) }
    def >>> (out: OutputStream): Unit = x (req) ok { _.writeTo(out) }
  }
}

import org.apache.http.conn.{Scheme,SchemeRegistry,PlainSocketFactory}
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager

object Http extends Http {
  override def createClientConnectionManager() = {
    val registry = new SchemeRegistry()
    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
    registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
    new ThreadSafeClientConnManager(getParams(), registry)
  }
}