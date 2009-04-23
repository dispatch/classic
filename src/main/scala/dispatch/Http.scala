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
    Http.log.info("%s %s", req.getMethod, req.getURI)
    host match {
      case None => client.execute(req)
      case Some(host) => client.execute(host, req)
    }
  }
  /** Sets authentication credentials for bound host. */
  def as (name: String, pass: String) = new Http(host, headers, Some((name, pass)))
  /** Add header */
  def << (k: String, v: String) = new Http(host, (k,v) :: headers, creds)
    
  /** eXecute wrapper */
  def x [T](req: HttpUriRequest) = new {
    /** handle response codes, response, and entity in block */
    def apply(block: (Int, HttpResponse, Option[HttpEntity]) => T) = {
      val res = execute(req)
      val ent = res.getEntity match {
        case null => None
        case ent => Some(ent)
      }
      try { block(res.getStatusLine.getStatusCode, res, ent) }
      finally { ent foreach (_.consumeContent) }
    }
    
    /** Handle reponse entity in block if reponse code returns true from chk. */
    def when(chk: Int => Boolean)(block: (HttpResponse, Option[HttpEntity]) => T) = this { (code, res, ent) => 
      if (chk(code)) block(res, ent)
      else throw StatusCode(code,
        ent.map(EntityUtils.toString(_, HTTP.UTF_8)).getOrElse("")
      )
    }
    
    /** Handle reponse entity in block when response code is 200 - 204 */
    def ok = (this when {code => (200 to 204) contains code}) _
  }
  
  /** Return wrapper for basic request workflow. */
  def apply(uri: String) = new Request(uri)
  
  /** Wrapper to handle common requests, preconfigured as response wrapper for a 
    * get request but defs return other method responders. */
  class Request(req: HttpUriRequest)  {
    headers foreach { case (k, v) => req.addHeader(k, v) }
    def this(uri: String) = this(new HttpGet(uri))

    def apply(block: T => Request) = new   

    /** Put the given object.toString and return response wrapper. */
    def <<< (body: Any) = {
      val m = new HttpPut(req.getURI)
      m setEntity new StringEntity(body.toString, HTTP.UTF_8)
      HttpProtocolParams.setUseExpectContinue(m.getParams, false)
      new Request(m)
    }
    /** Post the given key value sequence and return response wrapper. */
    def << (values: Map[String, Any]) = {
      val m = new HttpPost(req.getURI)
      m setEntity new UrlEncodedFormEntity(Http.map2ee(values), HTTP.UTF_8)
      new Request(m)
    }
    /** Get with query parameters */
    def ?< (values: Map[String, Any]) = if(values.isEmpty) this else
      new Request(new HttpGet(req.getURI + Http ? (values)))
    /** HTTP Delete request. */
    def --() = new Request(new HttpDelete(req.getURI))
    
    def x (Http: http) = new { // Response?
    
    /** Execute and process response in block */
    def apply [T] (block: (Int, HttpResponse, Option[HttpEntity]) => T) = x (req) (block)
    /** Handle response and entity in block if OK. */
    def ok [T] (block: (HttpResponse, Option[HttpEntity]) => T) = x (req) ok (block)
    /** Handle response entity in block if OK. */
    def okee [T] (block: HttpEntity => T): T = ok { 
      case (_, Some(ent)) => block(ent)
      case (res, _) => error("response has no entity: " + res)
    }
    /** Handle InputStream in block if OK. */
    def >> [T] (block: InputStream => T) = okee (ent => block(ent.getContent))
    /** Return response in String if OK. (Don't blow your heap, kids.) */
    def as_str = okee { EntityUtils.toString(_, HTTP.UTF_8) }
    /** Write to the given OutputStream. */
    def >>> [OS <: OutputStream](out: OS) = { okee { _.writeTo(out) } ; out }
    /** Process response as XML document in block */
    def <> [T] (block: (scala.xml.NodeSeq => T)) = { 
      // an InputStream source is the right way, but ConstructingParser
      // won't let us peek and we're tired of trying
      val full_in = as_str
      val in = full_in.substring(full_in.indexOf('<')) // strip any garbage
      val src = scala.io.Source.fromString(in)
      block(scala.xml.parsing.XhtmlParser(src))
    }
    /** Process response as JsValue in block */
    def $ [T](block: json.Js.JsF[T]): T = >> { stm => block(json.Js(stm)) }

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

/** May be used directly from any thread. */
object Http extends Http(None, Nil, None) {
  import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
  import org.apache.http.conn.ssl.SSLSocketFactory
  import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager

  override lazy val client = new ConfiguredHttpClient {
    override def createClientConnectionManager() = {
      val registry = new SchemeRegistry()
      registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
      registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
      new ThreadSafeClientConnManager(getParams(), registry)
    }
  }
  val log = net.lag.logging.Logger.get
  /** Convert repeating name value tuples to list of pairs for httpclient */
  private def map2ee(values: Map[String, Any]) = 
    new java.util.ArrayList[BasicNameValuePair](values.size) {
      values.foreach { case (k, v) => add(new BasicNameValuePair(k, v.toString)) }
    }
  /** Produce formatted query strings from a Map of parameters */
  def ?(values: Map[String, Any]) = if (values.isEmpty) "" else 
    "?" + URLEncodedUtils.format(map2ee(values), HTTP.UTF_8)
}
