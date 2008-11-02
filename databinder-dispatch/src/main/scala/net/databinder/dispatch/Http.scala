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
  
  /** Execute in HttpClient. */
  protected def execute(req: HttpUriRequest) = client.execute(req)
  
  /** Get wrapper */
  def g [T](uri: String) = x[T](new HttpGet(uri))
  
  /** eXecute wrapper */
  def x [T](req: HttpUriRequest) = new {
    /** handle response codes, response, and entity in thunk */
    def apply(thunk: (Int, HttpResponse, HttpEntity) => T) = {
      val res = execute(req)
      res.getEntity match {
        case null => error("no response message")
        case ent => try { 
            thunk(res.getStatusLine.getStatusCode, res, ent)
          } finally { ent.consumeContent() }
      }
    }
    
    /** Handle reponse entity in thunk if reponse code returns true from chk. */
    def when(chk: Int => Boolean)(thunk: HttpEntity => T) = this { (code, res, ent) => 
      if (chk(code))
        thunk(ent)
      else error("Response not OK: " + code + " , body:\n" + EntityUtils.toString(ent))
    }
    
    /** Handle reponse entity in thunk when response code is 200 - 204 */
    def ok = (this when {code => (200 to 204) contains code}) _
  }
  
  /** Return wrapper for basic request workflow. */
  def apply(uri: String) = new Request(uri)
  
  /** Wrapper to handle common requests, preconfigured as response wrapper for a 
    * get request but defs return other method responders. */
  class Request(uri: String) extends Respond(new HttpGet(uri)) {
    /** Put the given object.toString and return response wrapper. */
    def <<< (body: Any) = {
      val m = new HttpPut(uri)
      m setEntity new StringEntity(body.toString, HTTP.UTF_8)
      HttpProtocolParams.setUseExpectContinue(m.getParams, false)
      new Respond(m)
    }
    /** Post the given key value sequence and return response wrapper. */
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
    /** Post the given map and return response wrapper. */
    def << (values: Map[String, Any]): Respond = <<(values.toList: _*)
  }
  /** Wrapper for common response handling. */
  class Respond(req: HttpUriRequest) {
    /** Handle InputStream in thunk if OK. */
    def >> [T] (thunk: InputStream => T) = x (req) ok (res => thunk(res.getContent))
    /** Return response in String if OK. (Don't blow your heap, kids.) */
    def as_str = x (req) ok { EntityUtils.toString(_) }
    /** Write to the given OutputStream. */
    def >>> (out: OutputStream): Unit = x (req) ok { _.writeTo(out) }
    /** Process response as XML document in thunk */
    def <> [T] (thunk: (scala.xml.Document => T)) = >> { is => 
      val bis = new java.io.BufferedInputStream(is)
      // walk through possible byte order mark garbage
      bis.mark(2)
      while (List(0xef, 0xbb, 0xbf) contains bis.read) bis.mark(2)
      bis.reset()

      val src = scala.io.Source.fromInputStream(bis)
      thunk(scala.xml.parsing.ConstructingParser.fromSource(src, false).document)
    }
  }
}

/** DefaultHttpClient with parameters that may be more widely compatible. */
class ConfiguredHttpClient extends DefaultHttpClient {
  override def createHttpParams = {
    val params = new BasicHttpParams
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
    HttpProtocolParams.setContentCharset(params, HTTP.UTF_8)
    HttpProtocolParams.setUseExpectContinue(params, false)
    params
  }
}

/** Instances to be used by a single thread, with thunk to run before any execute. */
class SingleHttp(x_action: (HttpRequest) => Unit) extends Http {
  lazy val client = new ConfiguredHttpClient
  def on_x(new_x_action: (HttpRequest) => Unit) = 
    new SingleHttp(new_x_action(_))

  override def execute(req: HttpUriRequest) = {
    x_action(req)
    super.execute(req)
  }
}

/** Instances to be used by a single thread, with thunk and host to be used for any execute. */
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

/** May be used directly from any thread, or to return configured single-thread instances. */
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
