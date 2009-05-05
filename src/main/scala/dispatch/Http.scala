package dispatch

import util.DynamicVariable
import java.io.{InputStream,OutputStream,BufferedInputStream,BufferedOutputStream}
import java.net.URI

import org.apache.http._
import org.apache.http.client._
import org.apache.http.impl.client.{DefaultHttpClient, BasicCredentialsProvider}
import org.apache.http.client.methods._
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.utils.URLEncodedUtils

import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.{HTTP, HttpContext}
import org.apache.http.params.{HttpProtocolParams, BasicHttpParams}
import org.apache.http.util.EntityUtils
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials, Credentials}

case class StatusCode(code: Int, contents:String)
  extends Exception("Exceptional resoponse code: " + code + "\n" + contents)

class Http {
  val credentials = new DynamicVariable[Option[(AuthScope, Credentials)]](None)
  val client = new ConfiguredHttpClient
  
  def credentialsProvider = new BasicCredentialsProvider {
    override def getCredentials(scope: AuthScope) = null
  }
  /** Execute method for the given host, with logging. */
  def execute(host: HttpHost, req: HttpUriRequest) = {
    Http.log.info("%s %s%s", req.getMethod, host, req.getURI)
    client.execute(host, req) 
  }
  /** Execute for given optional parametrs, with logging. Creates local scope for credentials. */
  def execute: (Option[HttpHost], Option[Credentials], HttpUriRequest) => HttpResponse = {
    case (Some(host), Some(creds), req) =>
      client.credentials.withValue(Some((new AuthScope(host.getHostName, host.getPort), creds)))(execute(host, req))
    case (Some(host), _, req) => execute(host, req)
    case (_, _, req) => 
      Http.log.info("%s %s", req.getMethod, req.getURI)
      client.execute(req)
  }
  /** Execute request and handle response codes, response, and entity in block */
  def x [T](req: Request)(block: (Int, HttpResponse, Option[HttpEntity]) => T) = {
    val res = execute(req.host, req.creds, req.req)
    val ent = res.getEntity match {
      case null => None
      case ent => Some(ent)
    }
    try { block(res.getStatusLine.getStatusCode, res, ent) }
    finally { ent foreach (_.consumeContent) }
  }
  /** Apply Response Handler if reponse code returns true from chk. */
  def when[T](chk: Int => Boolean)(hand: Handler[T]) = x(hand.req) {
    case (code, res, ent) if chk(code) => hand.block(code, res, ent)
    case (code, _, Some(ent)) => throw StatusCode(code, EntityUtils.toString(ent, HTTP.UTF_8))
    case (code, _, _)         => throw StatusCode(code, "[no entity]")
  }
  /** Apply a custom block in addition to predefined response Handler. */
  def also[T](block: (Int, HttpResponse, Option[HttpEntity]) => T)(hand: Handler[T]) = 
    x(hand.req) { (code, res, ent) => (block(code, res, ent), hand.block(code, res, ent) ) }
  
  /** Apply handler block when response code is 200 - 204 */
  def apply[T](hand: Handler[T]) = (this when {code => (200 to 204) contains code})(hand)

  /** Back-apply Http block */
  def apply[T](block: Http => T) = block(this)
}

/* Factory for requests from a host */
object :/ {
  def apply(hostname: String, port: Int): Request = 
    new Request(Some(new HttpHost(hostname, port)), None, Nil)

  def apply(name: String): Request = apply(name, 80)
}

/** Factory for requests from the root. */
object / {
  def apply(path: String) = new Request("/" + path)
}

object Request {
  /** Request transformer */
  type Xf = HttpRequestBase => HttpRequestBase
  /** Updates the request URI with the given string-to-string  function. (mutates request) */
  def uri_xf(sxf: String => String)(req: HttpRequestBase) = {
    req.setURI(URI.create(sxf(req.getURI.toString)))
    req
  }
}

case class Handler[T](req: Request, block: (Int, HttpResponse, Option[HttpEntity]) => T)
object Handler { 
  def apply[T](req: Request, block: HttpEntity => T): Handler[T] = 
    Handler(req, { (code, res, ent) => ent match {
      case Some(ent) => block(ent) 
      case None => error("response has no entity: " + res)
    } } )
}

object /\ extends Request("")

class Request(val host: Option[HttpHost], val creds: Option[Credentials], val xfs: List[Request.Xf]) {

  /** Construct with path or full URI. */
  def this(str: String) = this(None, None, Request.uri_xf(cur => str)_ :: Nil)
  
  /** Construct as a clone, e.g. in class extends clause. */
  def this(req: Request) = this(req.host, req.creds, req.xfs)
  
  private def next(xf: Request.Xf) = new Request(host, creds, xf :: xfs)
  private def next_uri(sxf: String => String) = next(Request.uri_xf(sxf))
  
  private def mimic(dest: HttpRequestBase)(req: HttpRequestBase) = {
    dest.setURI(req.getURI)
    dest.setHeaders(req.getAllHeaders)
    dest
  }
  
  def as (name: String, pass: String) = 
    new Request(host, Some(new UsernamePasswordCredentials(name, pass)), xfs)
  
  /** Combine two requests, i.e. separately constructed host and path specs. */
  def / (req: Request) = new Request(host orElse req.host, creds orElse req.creds, xfs ::: req.xfs)

  /** Append an element to this request's path. (mutates request) */
  def / (path: String) = next_uri { _ + "/" + path }
  
  /** Add headers to this request. (mutates request) */
  def <:< (values: Map[String, String]) = next { req =>
    values foreach { case (k, v) => req.addHeader(k, v) }
    req
  }

  /** Put the given object.toString and return response wrapper. (new request, old URI) */
  def <<< (body: Any) = next {
    val m = new HttpPut
    m setEntity new StringEntity(body.toString, HTTP.UTF_8)
    HttpProtocolParams.setUseExpectContinue(m.getParams, false)
    mimic(m)_
  }
  /** Post the given key value sequence and return response wrapper. (new request, old URI) */
  def << (values: Map[String, Any]) = next {
    val m = new HttpPost
    m setEntity new UrlEncodedFormEntity(Http.map2ee(values), HTTP.UTF_8)
    mimic(m)_
  }
  
  /** Add query parameters. (mutates request) */
  def <<? (values: Map[String, Any]) = next_uri { uri =>
    if(values.isEmpty) uri
    else uri + Http ? (values)
  }
  
  /** HTTP Delete request. (new request, old URI) */
  def <--() = next { mimic(new HttpDelete)_ }

  /** Builds underlying request starting with a blank get and applying transformers right to left. */
  lazy val req = {
    val start: HttpRequestBase = new HttpGet("")
    (xfs :\ start) { (a,b) => a(b) }
  }

  /** Handle InputStream in block if OK. */
  def >> [T] (block: InputStream => T) = Handler(this, { ent => block(ent.getContent) })
  /** Return response in String if OK. (Don't blow your heap, kids.) */
  def as_str = Handler(this, ent => EntityUtils.toString(ent, HTTP.UTF_8))
  /** Write to the given OutputStream. */
  def >>> [OS <: OutputStream](out: OS) = Handler(this, { ent => ent.writeTo(out); out })
  /** Process response as XML document in block */
  def <> [T] (block: xml.NodeSeq => T) = >> { stm => block(xml.XML.load(stm)) }
  
  /** Process response as JsValue in block */
  def ># [T](block: json.Js.JsF[T]) = >> { stm => block(json.Js(stm)) }
  
  /** Ignore response body if OK. */
  def >| = Handler(this, ent => ())
}


class ConfiguredHttpClient extends DefaultHttpClient { 
  override def createHttpParams = {
    val params = new BasicHttpParams
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
    HttpProtocolParams.setContentCharset(params, HTTP.UTF_8)
    HttpProtocolParams.setUseExpectContinue(params, false)
    params
  }
  val credentials = new DynamicVariable[Option[(AuthScope, Credentials)]](None)
  setCredentialsProvider(new BasicCredentialsProvider {
    override def getCredentials(scope: AuthScope) = credentials.value match {
      case Some((auth_scope, creds)) if scope.`match`(auth_scope) >= 0 => creds
      case _ => null
    }
  })
}

/** May be used directly from any thread. */
object Http extends Http {
  import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
  import org.apache.http.conn.ssl.SSLSocketFactory
  import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
  
  /** import to support e.g. Http("http://example.com/" >>> System.out) */
  implicit def str2req(str: String) = new Request(str)

  override val client = new ConfiguredHttpClient {
    override def createClientConnectionManager() = {
      val registry = new SchemeRegistry()
      registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
      registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))
      new ThreadSafeClientConnManager(getParams(), registry)
    }
  }
  /** Shutdown connection manager, threads. (Needed to close console cleanly.) */
  def shutdown() = client.getConnectionManager.shutdown()

  val log = net.lag.logging.Logger.get
  /** Convert repeating name value tuples to list of pairs for httpclient */
  def map2ee(values: Map[String, Any]) = 
    new java.util.ArrayList[BasicNameValuePair](values.size) {
      values.foreach { case (k, v) => add(new BasicNameValuePair(k, v.toString)) }
    }
  /** Produce formatted query strings from a Map of parameters */
  def ?(values: Map[String, Any]) = if (values.isEmpty) "" else 
    "?" + URLEncodedUtils.format(map2ee(values), HTTP.UTF_8)
}
