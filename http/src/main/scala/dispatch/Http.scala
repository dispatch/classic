package dispatch

import io.Source
import collection.Map
import collection.immutable.{Map => IMap}
import util.DynamicVariable
import java.io.{InputStream,OutputStream,BufferedInputStream,BufferedOutputStream,File}
import java.net.URI
import java.util.zip.GZIPInputStream

import org.apache.http._
import org.apache.http.client._
import org.apache.http.impl.client.{DefaultHttpClient, BasicCredentialsProvider}
import org.apache.http.client.methods._
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.utils.URLEncodedUtils

import org.apache.http.entity.{StringEntity,FileEntity}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.params.{HttpProtocolParams, BasicHttpParams}
import org.apache.http.util.EntityUtils
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials, Credentials}

case class StatusCode(code: Int, contents:String)
  extends Exception("Exceptional resoponse code: " + code + "\n" + contents)

/** Simple info logger */
trait Logger { def info(msg: String, items: Any*) }

/** Http access point. Standard instances to be used by a single thread. */
class Http {
  val credentials = new DynamicVariable[Option[(AuthScope, Credentials)]](None)
  val client = new ConfiguredHttpClient
  
  def credentialsProvider = new BasicCredentialsProvider {
    override def getCredentials(scope: AuthScope) = null
  }
  
  /** Info Logger for this instance, default returns Connfiggy if on classpath else console logger. */
  lazy val log: Logger = try {
    new Logger {
      def getObject(name: String) = Class.forName(name + "$").getField("MODULE$").get(null)
      // using delegate, repeating parameters aren't working with structural typing in 2.7.x
      val delegate = getObject("net.lag.logging.Logger")
        .asInstanceOf[{ def get(n: String): { def ifInfo(o: => Object) } }]
        .get(classOf[Http].getCanonicalName)
      def info(msg: String, items: Any*) { delegate.ifInfo(msg.format(items: _*)) }
    }
  } catch {
    case e: ClassNotFoundException => new Logger {
      def info(msg: String, items: Any*) { 
        println("INF: [console logger] dispatch: " + msg.format(items: _*)) 
      }
    }
  }
  
  /** Execute method for the given host, with logging. */
  def execute(host: HttpHost, req: HttpUriRequest): HttpResponse = {
    log.info("%s %s%s", req.getMethod, host, req.getURI)
    client.crutchExecute(host, req)
  }
  /** Execute for given optional parametrs, with logging. Creates local scope for credentials. */
  val execute: (Option[HttpHost], Option[Credentials], HttpUriRequest) => HttpResponse = {
    case (Some(host), Some(creds), req) =>
      client.credentials.withValue(Some((new AuthScope(host.getHostName, host.getPort), creds)))(execute(host, req))
    case (None, Some(creds), _) => error("Credentials specified without explicit host")
    case (Some(host), _, req) => execute(host, req)
    case (_, _, req) => 
      log.info("%s %s", req.getMethod, req.getURI)
      client.execute(req)
  }
  /** Execute full request-response handler. */
  def x[T](hand: Handler[T]): T = x(hand.request)(hand.block)
  /** Execute request and handle response codes, response, and entity in block */
  def x [T](req: Request)(block: Handler.F[T]) = {
    val res = execute(req.host, req.creds, req.req)
    val ent = res.getEntity match {
      case null => None
      case ent => Some(ent)
    }
    try { block(res.getStatusLine.getStatusCode, res, ent) }
    finally { ent foreach (_.consumeContent) }
  }
  /** Apply Response Handler if reponse code returns true from chk. */
  def when[T](chk: Int => Boolean)(hand: Handler[T]) = x(hand.request) {
    case (code, res, ent) if chk(code) => hand.block(code, res, ent)
    case (code, _, Some(ent)) => throw StatusCode(code, EntityUtils.toString(ent, Request.factoryCharset))
    case (code, _, _)         => throw StatusCode(code, "[no entity]")
  }
  /** Apply a custom block in addition to predefined response Handler. */
  def also[A,B](hand: Handler[B])(block: Handler.F[A]) = 
    x(hand.request) { (code, res, ent) => ( hand.block(code, res, ent), block(code, res, ent) ) }
  
  /** Apply handler block when response code is 200 - 204 */
  def apply[T](hand: Handler[T]) = (this when {code => (200 to 204) contains code})(hand)
}

/** Nil request, useful to kick off a descriptors that don't have a factory. */
object /\ extends Request(None)

/* Factory for requests from a host */
object :/ {
  def apply(hostname: String, port: Int): Request = 
    new Request(Some(new HttpHost(hostname, port)))

  def apply(hostname: String): Request = new Request(Some(new HttpHost(hostname)))
}

/** Factory for requests from a directory, prepends '/'. */
object / {
  def apply(path: String) = /\ / path
}

object Request {
  /** Request transformer */
  type Xf = HttpRequestBase => HttpRequestBase
  /** Updates the request URI with the given string-to-string  function. (mutates request) */
  def uri_xf(sxf: String => String)(req: HttpRequestBase) = {
    req.setURI(URI.create(sxf(req.getURI.toString)))
    req
  }
  def mimic[T <: HttpRequestBase](dest: T)(req: HttpRequestBase) = {
    dest.setURI(req.getURI)
    dest.setHeaders(req.getAllHeaders)
    dest
  }
  /** Dispatch's factory-default charset, utf-8 */
  val factoryCharset = org.apache.http.protocol.HTTP.UTF_8
}

/** Request handler, contains request descriptor and a function to transform the result. */
case class Handler[T](request: Request, block: Handler.F[T]) extends Handlers {
  /** Create a new handler with block that receives all response parameters and
      this handler's block converted to parameterless function. */
  def apply[R](next: (Int, HttpResponse, Option[HttpEntity], () => T) => R) =
    new Handler(request, {(code, res, ent) =>
      next(code, res, ent, () => block(code, res, ent))
    })
}

object Handler { 
  type F[T] = (Int, HttpResponse, Option[HttpEntity]) => T
  /** Turns a simple entity handler in into a full response handler that fails if no entity */
  def apply[T](req: Request, block: HttpEntity => T): Handler[T] = 
    Handler(req, { (code, res, ent) => ent match {
      case Some(ent) => block(ent) 
      case None => error("response has no entity: " + res)
    } } )
}

/** Post method that produces updated, self-typed copies when new parameters are added */
trait Post[P <: Post[P]] extends HttpPost { self: P =>
  def values: Map[String, Any]
  def add(more: Map[String, Any]): P
}
/** Standard, URL-encoded form posting */
class SimplePost(val values: Map[String, Any]) extends Post[SimplePost] { 
  this setEntity new UrlEncodedFormEntity(Http.map2ee(values), Request.factoryCharset)
  def add(more: Map[String, Any]) = new SimplePost(IMap.empty ++ values ++ more.elements)
}

/** Request descriptor, possibly contains a host, credentials, and a list of transformation functions. */
class Request(
  val host: Option[HttpHost], 
  val creds: Option[Credentials], 
  val xfs: List[Request.Xf], 
  val defaultCharset: String
) extends Handlers {
  /** Construct with path or full URI. */
  def this(str: String) = this(None, None, Request.uri_xf(cur => cur + str)_ :: Nil, Request.factoryCharset)
  
  /** Construct as a clone, e.g. in class extends clause. */
  def this(req: Request) = this(req.host, req.creds, req.xfs, req.defaultCharset)

  /** Construct with host only. */
  def this(host: Option[HttpHost]) = this(host, None, Nil, Request.factoryCharset)
  
  def next(xf: Request.Xf) = new Request(host, creds, xf :: xfs, defaultCharset)
  def next_uri(sxf: String => String) = next(Request.uri_xf(sxf))
    
  // The below functions create new request descriptors based off of the current one.
  // Most are intended to be used as infix operators; those that don't take a parameter
  // have character names to be used with dot notation, e.g. :/("example.com").HEAD.secure >>> {...}
  
  /** Set credentials to be used for this request; requires a host value :/(...) upon execution. */
  def as (name: String, pass: String) = 
    new Request(host, Some(new UsernamePasswordCredentials(name, pass)), xfs, defaultCharset)
  
  /** Convert this to a secure (scheme https) request if not already */
  def secure = new Request(host map { 
    h => new HttpHost(h.getHostName, h.getPort, "https") // default port -1 works for either
  } orElse { error("secure requires an explicit host") }, creds, xfs, defaultCharset)
  
  /** Combine this request with another. */
  def <& (req: Request) = new Request(host orElse req.host, creds orElse req.creds, req.xfs ::: xfs, defaultCharset)
  
  /** Set the default character set to be used when processing the request in Handler#>> and
    derived operations >~, as_str, etc. (The 'factory' default is utf-8.) */
  def >\ (charset: String) = new Request(host, creds, xfs, charset)
  
  /** Combine this request with another handler. */
  def >& [T] (other: Handler[T]) = new Handler(this <& other.request, other.block)
  
  /** Append an element to this request's path, joins with '/'. (mutates request) */
  def / (path: String) = next_uri { _ + "/" + path }
  
  /** Add headers to this request. (mutates request) */
  def <:< (values: Map[String, String]) = next { req =>
    values foreach { case (k, v) => req.addHeader(k, v) }
    req
  }

  /* Add a gzip acceptance header */
  def gzip = this <:< IMap("Accept-Encoding" -> "gzip")

  /** Put the given object.toString and return response wrapper. (new request, mimics) */
  def <<< (body: Any) = next {
    val m = new HttpPut
    m setEntity new StringEntity(body.toString, Request.factoryCharset)
    HttpProtocolParams.setUseExpectContinue(m.getParams, false)
    Request.mimic(m)_
  }
  /** Put the given file and return response wrapper. (new request, mimics) */
  def <<< (file: File, content_type: String) = next {
    val m = new HttpPut
    m setEntity new FileEntity(file, content_type)
    Request.mimic(m) _
  }
  /** Post the given key value sequence and return response wrapper. (new request, mimics) */
  def << (values: Map[String, Any]) = next { 
    case p: Post[_] => p.add(values)
    case r => Request.mimic(new SimplePost(values))(r)
  }
  
  /** Add query parameters. (mutates request) */
  def <<? (values: Map[String, Any]) = next_uri { uri =>
    if (values.isEmpty) uri
    else uri + (
      if (uri contains '?') '&' + Http.q_str(values) else (Http ? values)
    )
  }
  
  // generators that change request method without adding parameters
  
  /** HTTP post request. (new request, mimics) */
  def POST = next { Request.mimic(new SimplePost(IMap.empty))_ }
    
  /** HTTP delete request. (new request, mimics) */
  def DELETE = next { Request.mimic(new HttpDelete)_ }
  
  /** HTTP head request. (new request, mimics). See >:> to access headers. */
  def HEAD = next { Request.mimic(new HttpHead)_ }

  // end Request generators

  /** Builds underlying request starting with a blank get and applying transformers right to left. */
  def req = {
    val start: HttpRequestBase = new HttpGet("")
    (xfs :\ start) { (a,b) => a(b) }
  }
  
  /** @return URI based on this request, e.g. if needed outside Disptach. */
  def to_uri = Http.to_uri(host, req)
  
  /** Use this request for trait Handlers */
  val request = this
}
trait Handlers {
  /** the below functions produce Handlers based on this request descriptor */
  val request: Request
  
  /** Handle InputStream in block, handle gzip if so encoded. Passes on any charset
      header value from response, otherwise the default charset. (See Request#>\) */
  def >> [T] (block: (InputStream, String) => T) = Handler(request, { ent =>
    val stm = (ent.getContent, ent.getContentEncoding) match {
      case (stm, null) => stm
      case (stm, enc) if enc.getValue == "gzip" => new GZIPInputStream(stm)
      case (stm, _) => stm
    }
    val charset = EntityUtils.getContentCharSet(ent) match {
      case null => request.defaultCharset
      case charset => charset
    }
    block(stm, charset)
  } )
  /** Handle InputStream in block, handle gzip if so encoded. */
  def >> [T] (block: InputStream => T): Handler[T] = >> { (stm, charset) => block(stm) }
  /** Handle response as a scala.io.Source, in a block. Note that Source may fail if the 
      character set it receives (determined in >>) is incorrect. To process resources
      that have incorrect charset headers, use >> ((InputStream, String) => T). */
  def >~ [T] (block: Source => T) = >> { (stm, charset) => 
    // 2.8 only: block(Source.fromInputStream(stm)(charset)
    import java.io._
    def read(reader: BufferedReader, buf: StringBuilder) {
      val line = reader.readLine()
      if (line != null)
        read(reader, buf.append(line + "\n"))
    }
    val buf = new StringBuilder()
    read(new BufferedReader(new InputStreamReader(stm, charset)), buf)
    block(Source.fromString(buf.toString))
  }
  /** Return response as a scala.io.Source. Charset note in >~  applies. */
  def as_source = >~ { so => so }
  /** Handle some non-huge response body as a String, in a block. Charset note in >~  applies. */
  def >- [T] (block: String => T) = >~ { so => block(so.mkString) }
  /** Return some non-huge response as a String. Charset note in >~  applies.*/
  def as_str = >- { s => s }
  /** Write to the given OutputStream. */
  def >>> [OS <: OutputStream](out: OS) = Handler(request, { ent => ent.writeTo(out); out })
  /** Process response as XML document in block */
  def <> [T] (block: xml.Elem => T) = >> { stm => block(xml.XML.load(stm)) }
  
  /** Process header as Map in block. Map returns empty set for header name misses. */
  def >:> [T] (block: IMap[String, Set[String]] => T) = 
    Handler(request, (_, res, _) => 
      block((IMap[String, Set[String]]().withDefaultValue(Set()) /: res.getAllHeaders) { 
        (m, h) => m + (h.getName -> (m(h.getName) + h.getValue))
      } )
    )
  
  /** Ignore response body. */
  def >| = Handler(request, (code, res, ent) => ())

  /** Split into two request handlers, return results of each in tuple. */
  def >+ [A, B] (block: Handlers => (Handler[A], Handler[B])) = {
    new Handler[(A,B)] ( request, { (code, res, opt_ent) =>
      val (a, b) = block(new Handlers { val request = /\ })
      (a.block(code, res, opt_ent), b.block(code,res,opt_ent))
    } )
  }
}

/** Basic extension of DefaultHttpClient defaulting to Http 1.1, UTF8, and no Expect-Continue.
    Scopes authorization credentials to particular requests thorugh a DynamicVariable. */
class ConfiguredHttpClient extends HttpCrutch { 
  override def createHttpParams = {
    val params = new BasicHttpParams
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
    HttpProtocolParams.setContentCharset(params, Request.factoryCharset)
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

/** Used by client APIs to build Handler or other objects via chaining, completed implicitly.
  * @see Http#builder2product */
trait Builder[T] { def product:T }

/** May be used directly from any thread. */
object Http extends Http {
  import org.apache.http.conn.scheme.{Scheme,SchemeRegistry,PlainSocketFactory}
  import org.apache.http.conn.ssl.SSLSocketFactory
  import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
  
  /** import to support e.g. Http("http://example.com/" >>> System.out) */
  implicit def str2req(str: String) = new Request(str)
  
  implicit def builder2product[T](builder: Builder[T]) = builder.product

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

  /** Convert repeating name value tuples to list of pairs for httpclient */
  def map2ee(values: Map[String, Any]) = java.util.Arrays asList (
    values.toSeq map { case (k, v) => new BasicNameValuePair(k, v.toString) } toArray : _*
  )
  /** @return %-encoded string for use in URLs */
  def % (s: String) = java.net.URLEncoder.encode(s, Request.factoryCharset)

  /** @return %-decoded string e.g. from query string or form body */
  def -% (s: String) = java.net.URLDecoder.decode(s, Request.factoryCharset)
  
  /** @return formatted and %-encoded query string, e.g. name=value&name2=value2 */
  def q_str (values: Map[String, Any]) = URLEncodedUtils.format(map2ee(values), Request.factoryCharset)

  /** @return formatted query string prepended by ? unless values map is empty  */
  def ? (values: Map[String, Any]) = if (values.isEmpty) "" else "?" + q_str(values)
  
  /** @return URI built from HttpHost if present combined with a HttpClient request object. */
  def to_uri(host: Option[HttpHost], req: HttpRequestBase) =
    URI.create(host.map(_.toURI).getOrElse("")).resolve(req.getURI)
}
