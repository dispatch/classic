package net.databinder.dispatch

import java.io.{InputStream,OutputStream,BufferedInputStream,BufferedOutputStream}

import org.apache.http._
import org.apache.http.client._
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods._
import org.apache.http.client.entity.UrlEncodedFormEntity

import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.{HTTP, HttpContext}
import org.apache.http.params.{HttpProtocolParams, BasicHttpParams}
import org.apache.http.util.EntityUtils
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}

trait Http {
  val client: HttpClient
  
  def execute(req: HttpUriRequest) = client.execute(req)
  
  def x [T](req: HttpUriRequest) = new {
    def apply(thunk: (Int, HttpResponse, HttpEntity) => T) = {
      val res = execute(req)
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
      else error("Response not OK: " + code + " , body:\n" + EntityUtils.toString(ent))
    }
    
    def ok = (this when {code => (200 to 204) contains code}) _
  }
  def apply(uri: String) = new Request(uri)
  
  class Request(uri: String) extends Respond(new HttpGet(uri)) {
    def <<< (body: Any) = {
      val m = new HttpPut(uri)
      m setEntity new StringEntity(body.toString, HTTP.UTF_8)
      HttpProtocolParams.setUseExpectContinue(m.getParams, false)
      new Respond(m)
    }
    def << (values: (String, Any)*) = {
      val m = new HttpPost(uri)
      m setEntity new UrlEncodedFormEntity(
        java.util.Arrays.asList(
          (values map) { tup => new BasicNameValuePair(tup._1, tup._2.toString) }.toArray
        ),
        HTTP.UTF_8
      )
      new Respond(m)
    }
    def << (values: Map[String, Any]): Respond = <<(values.toList: _*)
  }
  class Respond(req: HttpUriRequest) {
    def >> [T] (thunk: InputStream => T) = x (req) ok (res => thunk(res.getContent))
    def as_str = x (req) ok { EntityUtils.toString(_) }
    def >>> (out: OutputStream): Unit = x (req) ok { _.writeTo(out) }
  }
}

class ConfiguredHttpClient extends DefaultHttpClient {
  override def createHttpParams = {
    val params = new BasicHttpParams
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
    HttpProtocolParams.setContentCharset(params, HTTP.UTF_8)
    HttpProtocolParams.setUseExpectContinue(params, false)
    params
  }
}

trait RequestAction extends Http

class SingleHttp(x_action: (HttpRequest) => Unit) extends Http {
  lazy val client = new ConfiguredHttpClient
  def on_x(new_x_action: (HttpRequest) => Unit) = 
    new SingleHttp(new_x_action(_))

  override def execute(req: HttpUriRequest) = {
    x_action(req)
    super.execute(req)
  }
}

class SingleHttpHost(host: HttpHost, x_action: (HttpRequest) => Unit) extends SingleHttp(x_action) {
  def auth(name: String, password: String) = {
    val c = new SingleHttpHost(host, x_action)
    c.client.getCredentialsProvider.setCredentials(
        new AuthScope(host.getHostName, host.getPort), 
        new UsernamePasswordCredentials(name, password)
    )
    c
  }
  override def on_x(new_x_action: (HttpRequest) => Unit) = 
    new SingleHttpHost(host, new_x_action(_))

  override def execute(req: HttpUriRequest):HttpResponse = {
    x_action(req)
    client.execute(host, req)
  }
}

import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager

object Http extends Http {
  def host(hostname: String) = 
    new SingleHttpHost(new HttpHost(hostname), foo => ())
  def host(hostname: String, port: Int) = 
    new SingleHttpHost(new HttpHost(hostname, port), foo => ())

  lazy val client = new ConfiguredHttpClient {
    override def createClientConnectionManager() = {
      val registry = new SchemeRegistry()
      registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
      registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
      new ThreadSafeClientConnManager(getParams(), registry)
    }
  }
}
