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

case class UnexpectedResponse(code: Int) extends Exception("Unexpected resoponse code: " + code)

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
      if (chk(code)) thunk(ent)
      else throw UnexpectedResponse(code)
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
          (values map { case (k, v) => new BasicNameValuePair(k, v.toString) }: _*)
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
    /** Ignore response body if OK. */
    def >| = x (req) ok (res => ())
    /** Return response in String if OK. (Don't blow your heap, kids.) */
    def as_str = x (req) ok { EntityUtils.toString(_, HTTP.UTF_8) }
    /** Write to the given OutputStream. */
    def >>> (out: OutputStream): Unit = x (req) ok { _.writeTo(out) }
    /** Process response as XML document in thunk */
    def <> [T] (thunk: (scala.xml.NodeSeq => T)) = { 
      // an InputStream source is the right way, but ConstructingParser
      // won't let us peek and we're tired of trying
      val full_in = as_str
      val in = full_in.substring(full_in.indexOf('<')) // strip any garbage
      val src = scala.io.Source.fromString(in)
      thunk(scala.xml.parsing.XhtmlParser(src))
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
trait SingleHttp extends Http {
  lazy val client = new ConfiguredHttpClient
}

/** Instances to be used by a single thread, with thunk and host to be used for any execute. */
class HttpServer(host: HttpHost) extends SingleHttp {
  def this(hostname: String, port: Int) = this(new HttpHost(hostname, port))
  def this(hostname: String) = this(new HttpHost(hostname))
  override def execute(req: HttpUriRequest):HttpResponse = {
    preflight(req)
    client.execute(host, req)
  }
  private var preflight = { req: HttpUriRequest => () }
  protected def preflight(action: HttpUriRequest => Unit) {
    preflight = action
  }
  protected def auth(name: String, password: String) {
    client.getCredentialsProvider.setCredentials(
      new AuthScope(host.getHostName, host.getPort), 
      new UsernamePasswordCredentials(name, password)
    )
  }
}

import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager

/** May be used directly from any thread, or to return configured single-thread instances. */
object Http extends Http {
  lazy val client = new ConfiguredHttpClient {
    override def createClientConnectionManager() = {
      val registry = new SchemeRegistry()
      registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
      registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
      new ThreadSafeClientConnManager(getParams(), registry)
    }
  }
}
