package dispatch

import java.io.{InputStream,OutputStream,BufferedInputStream,BufferedOutputStream}

import org.apache.http._
import org.apache.http.client._
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods._
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.utils.URLEncodedUtils

import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.{HTTP, HttpContext}
import org.apache.http.params.{HttpProtocolParams, BasicHttpParams}
import org.apache.http.util.EntityUtils
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}

case class StatusCode(code: Int, contents:String)
  extends Exception("Exceptional resoponse code: " + code + "\n" + contents)

class Http(
  val host: Option[HttpHost], 
  val headers: List[(String, String)],
  val creds: Option[(String, String)]
) {
  def this(host: HttpHost) = this(Some(host), Nil, None)
  def this(hostname: String, port: Int) = this(new HttpHost(hostname, port))
  def this(hostname: String) = this(new HttpHost(hostname))
  lazy val client = new ConfiguredHttpClient {
    for (h <- host; (name, password) <- creds) {
      getCredentialsProvider.setCredentials(
        new AuthScope(h.getHostName, h.getPort), 
        new UsernamePasswordCredentials(name, password)
      )
    }
  }

  /** Uses bound host server in HTTPClient execute. */
  def execute(req: HttpUriRequest):HttpResponse = {
    host match {
      case None => client.execute(req)
      case Some(host) => client.execute(host, req)
    }
  }
  /** Sets authentication credentials for bound host. */
  def as (name: String, pass: String) = new Http(host, headers, Some((name, pass)))
  /** Add header */
  def << (k: String, v: String) = new Http(host, (k,v) :: headers, creds)
  
  /** Get wrapper */
  def g [T](uri: String) = x[T](new HttpGet(uri))
  
  /** eXecute wrapper */
  def x [T](req: HttpUriRequest) = new {
    /** handle response codes, response, and entity in thunk */
    def apply(thunk: (Int, HttpResponse, Option[HttpEntity]) => T) = {
      val res = execute(req)
      val ent = res.getEntity match {
        case null => None
        case ent => Some(ent)
      }
      try { thunk(res.getStatusLine.getStatusCode, res, ent) }
      finally { ent foreach (_.consumeContent) }
    }
    
    /** Handle reponse entity in thunk if reponse code returns true from chk. */
    def when(chk: Int => Boolean)(thunk: (HttpResponse, Option[HttpEntity]) => T) = this { (code, res, ent) => 
      if (chk(code)) thunk(res, ent)
      else throw StatusCode(code,
        ent.map(EntityUtils.toString(_, HTTP.UTF_8)).getOrElse("")
      )
    }
    
    /** Handle reponse entity in thunk when response code is 200 - 204 */
    def ok = (this when {code => (200 to 204) contains code}) _
  }
  
  /** Return wrapper for basic request workflow. */
  def apply(uri: String) = new Request(uri)
  
  /** Wrapper to handle common requests, preconfigured as response wrapper for a 
    * get request but defs return other method responders. */
  class Request(req: HttpUriRequest)  {
    headers foreach { case (k, v) => req.addHeader(k, v) }
    def this(uri: String) = this(new HttpGet(uri))
    /** Put the given object.toString and return response wrapper. */
    def <<< (body: Any) = {
      val m = new HttpPut(req.getURI)
      m setEntity new StringEntity(body.toString, HTTP.UTF_8)
      HttpProtocolParams.setUseExpectContinue(m.getParams, false)
      new Request(m)
    }
    /** Convert repeating name value tuples to list of pairs for httpclient */
    private def map2ee(values: Map[String, Any]) = 
      new java.util.ArrayList[BasicNameValuePair](values.size) {
        values.foreach { case (k, v) => add(new BasicNameValuePair(k, v.toString)) }
      }
    /** Post the given key value sequence and return response wrapper. */
    def << (values: Map[String, Any]) = {
      val m = new HttpPost(req.getURI)
      m setEntity new UrlEncodedFormEntity(map2ee(values), HTTP.UTF_8)
      new Request(m)
    }
    /** Get with query parameters */
    def ?< (values: Map[String, Any]) =
      new Request(new HttpGet(req.getURI + "?" + URLEncodedUtils.format(
        map2ee(values), HTTP.UTF_8
      )))
    def apply [T] (thunk: (Int, HttpResponse, Option[HttpEntity]) => T) = x (req) (thunk)
    /** Handle response and entity in thunk if OK. */
    def ok [T] (thunk: (HttpResponse, Option[HttpEntity]) => T) = x (req) ok (thunk)
    /** Handle response entity in thunk if OK. */
    def okee [T] (thunk: HttpEntity => T): T = ok { 
      case (_, Some(ent)) => thunk(ent)
      case (res, _) => error("response has no entity: " + res)
    }
    /** Handle InputStream in thunk if OK. */
    def >> [T] (thunk: InputStream => T) = okee (ent => thunk(ent.getContent))
    /** Return response in String if OK. (Don't blow your heap, kids.) */
    def as_str = okee { EntityUtils.toString(_, HTTP.UTF_8) }
    /** Write to the given OutputStream. */
    def >>> (out: OutputStream): Unit = okee { _.writeTo(out) }
    /** Process response as XML document in thunk */
    def <> [T] (thunk: (scala.xml.NodeSeq => T)) = { 
      // an InputStream source is the right way, but ConstructingParser
      // won't let us peek and we're tired of trying
      val full_in = as_str
      val in = full_in.substring(full_in.indexOf('<')) // strip any garbage
      val src = scala.io.Source.fromString(in)
      thunk(scala.xml.parsing.XhtmlParser(src))
    }
    def $ [T](thunk: json.JsValue => T): T = >> { stm => thunk(json.Js(stm)) }

    /** Ignore response body if OK. */
    def >| = ok ((r,e) => ())
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

import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager

/** May be used directly from any thread, or to return configured single-thread instances. */
object Http extends Http(None, Nil, None) {
  override lazy val client = new ConfiguredHttpClient {
    override def createClientConnectionManager() = {
      val registry = new SchemeRegistry()
      registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
      registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
      new ThreadSafeClientConnManager(getParams(), registry)
    }
  }
}
