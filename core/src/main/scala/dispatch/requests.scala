package dispatch

import org.apache.http.message.BasicHttpRequest
import org.apache.http.{HttpEntity,HttpHost,HttpRequest}
import java.net.URI

object Request {
  /** Dispatch's factory-default charset, utf-8 */
  val factoryCharset = org.apache.http.protocol.HTTP.UTF_8
  /** Headers lists in reverse order */
  type Headers = List[(String, String)]
  def to_host(uristr: String) = {
    val uri = URI.create(uristr)
    new HttpHost(uri.getHost, uri.getPort, uri.getScheme)
  }
  def to_path(uristr: String) = {
    val uri = URI.create(uristr)
    (new URI(null, null, uri.getPath, uri.getQuery, null)).toString
  }
  /** @return formatted query string prepended by ? unless values map is empty  */
  def ? (values: Map[String, String]) = if (values.isEmpty) "" else "?" + q_str(values)

  /** @return formatted and %-encoded query string, e.g. name=value&name2=value2 */
  def q_str (values: Map[String, String]) = {
    val enc = java.net.URLEncoder.encode(_: String, Request.factoryCharset)
    values.map{ case (k,v) => enc(k) + "=" + enc(v) }.mkString("&")
  }

  /** @return %-encoded string for use in URLs */
  def % (s: String) = java.net.URLEncoder.encode(s, Request.factoryCharset)

  /** @return %-decoded string e.g. from query string or form body */
  def -% (s: String) = java.net.URLDecoder.decode(s, Request.factoryCharset)
}

/** Request descriptor, possibly contains a host, credentials, and a list of transformation functions. */
class Request(
  val host: HttpHost, 
  val creds: Option[Credentials], 
  val method: String,
  val path: String,
  val headers: Request.Headers,
  val defaultCharset: String
) {
  /** Construct with path or full URI. */
  def this(str: String) = {
    this(Request.to_host(str), None, "GET", Request.to_path(str), Nil, Request.factoryCharset)
  }
  
  /** Construct with host only. */
  def this(host: HttpHost) = this(host, None, "GET", "", Nil, Request.factoryCharset)

  /** Construct as a clone, e.g. in class extends clause. */
  def this(req: Request) =
    this(req.host, req.creds, req.method, req.path, req.headers, req.defaultCharset)
}

trait ImplicitCoreRequestOps {
  implicit def toCoreRequestOps (req: Request) = new Request(req) with CoreRequestOps
}

trait CoreRequestOps { self: Request =>
  // The below functions create new request descriptors based off of the current one.
  // Most are intended to be used as infix operators; those that don't take a parameter
  // have character names to be used with dot notation, e.g. :/("example.com").HEAD.secure >>> {...}
  
  /** Set credentials that may be used for basic or digest auth; requires a host value :/(...) upon execution. */
  def as (name: String, pass: String) = 
    new Request(host, Some(Credentials(name, pass)), method, path, headers, defaultCharset)

  /** Add basic auth header unconditionally to this request. Does not wait for a 401 response. */
  def as_! (name: String, pass: String) = error("need a base64 encoder here")
  /*this <:< Map("Authorization" -> (
    "Basic " + new String(org.apache.commons.codec.binary.Base64.encodeBase64(
      "%s:%s".format(name, pass).getBytes
    ))
  ))*/
  
  /** Convert this to a secure (scheme https) request if not already */
  def secure = new Request( 
    // default port -1 works for either
    new HttpHost(host.getHostName, host.getPort, "https"),
    creds, method, path, headers, defaultCharset
  )
  
  /** Combine this request with another. */
  def <& (req: Request) = new Request(
    if (req.host.getHostName.isEmpty) host else req.host, 
    req.creds orElse creds,
    req.method,
    if (req.path.isEmpty) path else req.path,
    req.headers ::: headers,
    if (Request.factoryCharset == req.defaultCharset) defaultCharset else req.defaultCharset
  )
  
  /** Set the default character set to be used when processing the request in <<, <<<, Handler#>> and
    derived operations >~, as_str, etc. (The 'factory' default is utf-8.) */
  def >\ (charset: String) = new Request(
    host, creds, method, path, headers, charset)
  
  /** Combine this request with another handler. */
  def >& [T] (other: Handler[T]) = new Handler(this <& other.request, other.block)
  
  /** Append an element to this request's path, joins with '/'. (mutates request) */
  def / (path: String) = new Request(
    host, creds, method, this.path + "/" + path, headers, defaultCharset)
  
  /** Add headers to this request. (mutates request) */
  def <:< (values: Map[String, String]) = new Request(
    host, creds, method, path, values.toList ::: headers, defaultCharset
  )

  /* Add a gzip acceptance header */
  def gzip = this <:< Map("Accept-Encoding" -> "gzip")

  /** Put the given string. (new request, mimics) */
  def <<< (body: String): Request =
    error("new StringEntity(body.toString, defaultCharset)")

  /** Put the given file. (new request, mimics) */
  def <<< (file: java.io.File, content_type: String) =
    error("new FileEntity(file, content_type)")

  /** Post the given key value sequence. (new request, mimics) */
  /*def << (values: Map[String, Any]) = next { 
    case p: Post[_] => Request.mimic(p.add(values))(p)
    case r => Request.mimic(new SimplePost(Map.empty ++ values, defaultCharset))(r)
  }*/
  /** Post the given string value. (new request, mimics) */
  /*def << (string_body: String) = next { 
    val m = new HttpPost
    m setEntity new StringEntity(string_body, defaultCharset)
    Request.mimic(m)_
  }*/
  
  /** Add query parameters. (mutates request) */
  def <<? (values: Map[String, String]) =
    if (values.isEmpty) this
    else new Request(
      host,
      creds,
      method,
      if (path contains '?') path + '&' + Request.q_str(values)
      else path + (Request ? values),
      headers,
      defaultCharset
    )
  
  private def method(method: String) = new Request(
    host, creds, method, path, headers, defaultCharset)
  
  /** HTTP post request. (new request, mimics) */
  def POST = method("POST")
    
  /** HTTP delete request. (new request, mimics) */
  def DELETE = method("DELETE")
  
  /** HTTP head request. (new request, mimics). See >:> to access headers. */
  def HEAD = method("HEAD")


  /** @return URI based on this request, e.g. if needed outside Disptach. */
  def to_uri = URI.create(host.toURI).resolve(path)
}

/** Nil request, useful to kick off a descriptors that don't have a factory. */
object /\ extends Request(new HttpHost(""))

/* Factory for requests from a host */
object :/ {
  def apply(hostname: String, port: Int): Request = 
    new Request(new HttpHost(hostname, port))

  def apply(hostname: String): Request = new Request(new HttpHost(hostname))
}

/** Factory for requests from a directory, prepends '/'. */
object / extends ImplicitCoreRequestOps {
  def apply(path: String) = /\ / path
}

case class Credentials(username: String, password: String)
