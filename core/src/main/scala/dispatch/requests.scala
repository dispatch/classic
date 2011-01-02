package dispatch

import org.apache.http.message.BasicHttpRequest
import org.apache.http.{HttpEntity,HttpHost,HttpRequest}
import org.apache.http.util.EntityUtils
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
}

/** Request descriptor, possibly contains a host, credentials, and a list of transformation functions. */
case class Request(
  val host: HttpHost, 
  val creds: Option[Credentials], 
  val method: String,
  val path: String,
  val headers: Request.Headers,
  val body: Option[HttpEntity],
  val defaultCharset: String
) {
  /** Construct with path or full URI. */
  def this(str: String) = {
    this(Request.to_host(str), None, "GET", Request.to_path(str), Nil, None, Request.factoryCharset)
  }
  
  /** Construct with host only. */
  def this(host: HttpHost) = this(host, None, "GET", "", Nil, None, Request.factoryCharset)

  /** Construct as a clone, e.g. in class extends clause. */
  def this(req: Request) =
    this(req.host, req.creds, req.method, req.path, req.headers, req.body, req.defaultCharset)

  // string encoding functions that depend on defaultCharset

  /** @return %-encoded string for use in URLs */
  def % (s: String) = java.net.URLEncoder.encode(s, defaultCharset)

  /** @return %-decoded string e.g. from query string or form body */
  def -% (s: String) = java.net.URLDecoder.decode(s, defaultCharset)

  /** @return formatted query string prepended by ? unless values map is empty  */
  def ? (values: Iterable[(String, String)]) =
    if (values.isEmpty) "" else "?" + form_enc(values)

  /** @return formatted and %-encoded query string, e.g. name=value&name2=value2 */
  def form_enc (values: Iterable[(String, String)]) = {
    form_join(values.map(form_elem))
  }
  def form_elem(value: (String, String)) = %(value._1) + "=" + %(value._2)
  def form_join(values: Iterable[String]) = values.mkString("&")
}

trait ImplicitRequestTerms {
  implicit def toRequestTerms (req: Request) = new RequestTerms(req)
}

object RequestTerms extends ImplicitRequestTerms

class RequestTerms(subject: Request) {
  // The below functions create new request descriptors based off of the current one.
  // Most are intended to be used as infix operators; those that don't take a parameter
  // have character names to be used with dot notation, e.g. :/("example.com").HEAD.secure >>> {...}
  
  /** Set credentials that may be used for basic or digest auth; requires a host value :/(...) upon execution. */
  def as (name: String, pass: String) = subject.copy(creds=Some(Credentials(name, pass)))

  /** Add basic auth header unconditionally to this request. Does not wait for a 401 response. */
  def as_! (name: String, pass: String) = {
    import javax.xml.bind.DatatypeConverter.{printBase64Binary=>base64}
    this <:< Map("Authorization" -> (
      "Basic " + new String(base64(
        "%s:%s".format(name, pass).getBytes
      ))
    ))
  }
  
  /** Convert this to a secure (scheme https) request if not already */
  def secure = subject.copy(
    // default port -1 works for either
    host=new HttpHost(subject.host.getHostName, subject.host.getPort, "https")
  )
  
  /** Combine this request with another. */
  def <& (req: Request) = Request(
    if (req.host.getHostName.isEmpty) subject.host else req.host, 
    req.creds orElse subject.creds,
    req.method,
    if (req.path.isEmpty) subject.path else req.path,
    req.headers ::: subject.headers,
    req.body orElse subject.body,
    if (Request.factoryCharset == req.defaultCharset) subject.defaultCharset else req.defaultCharset
  )
  
  /** Set the default character set to be used when processing the request in <<, <<<, Handler#>> and
    derived operations >~, as_str, etc. (The 'factory' default is utf-8.) */
  def >\ (charset: String) = subject.copy(defaultCharset=charset)
  
  /** Combine this request with another handler. */
  def >& [T] (other: Handler[T]) = new Handler(this <& other.request, other.block)
  
  /** Append an element to this request's path, joins with '/'. (mutates request) */
  def / (path: String) = subject.copy(path=subject.path + "/" + path)
  
  /** Add headers to this request. (mutates request) */
  def <:< (values: Map[String, String]) = subject.copy(
    headers=values.toList ::: subject.headers
  )

  /* Add a gzip acceptance header */
  def gzip = this <:< Map("Accept-Encoding" -> "gzip")

  /** Put the given string. */
  def <<< (stringbody: String): Request = PUT.copy(
    body=Some(new org.apache.http.entity.StringEntity(stringbody, subject.defaultCharset))
  )
  /** Put the given file. (new request, mimics) */
  def <<< (file: java.io.File, content_type: String) = PUT.copy(
    body=Some(new org.apache.http.entity.FileEntity(file, content_type))
  )

  /** Post the given key value sequence. (new request, mimics) */
  def << (values: Iterable[(String, String)]): Request = this << subject.form_join(
    (subject.body.map(EntityUtils.toString).filterNot { _.isEmpty }.toSeq ++
      values.map(subject.form_elem)
    )
  )

  /** Post the given string value. (new request, mimics) */
  def << (stringbody: String): Request = POST.copy(
    body=Some(new org.apache.http.entity.StringEntity(stringbody, subject.defaultCharset))
  )
  
  /** Add query parameters. (mutates request) */
  def <<? (values: Iterable[(String, String)]) =
    if (values.isEmpty) subject
    else subject.copy(
      path =
        if (subject.path contains '?') subject.path + '&' + subject.form_enc(values)
        else subject.path + (subject ? values)
    )
  
  /** HTTP post request. (new request, mimics) */
  def POST = subject.copy(method="POST")

  /** HTTP post request. (new request, mimics) */
  def PUT = subject.copy(method="PUT")
    
  /** HTTP delete request. (new request, mimics) */
  def DELETE = subject.copy(method="DELETE")
  
  /** HTTP head request. (new request, mimics). See >:> to access headers. */
  def HEAD = subject.copy(method="HEAD")


  /** @return URI based on this request, e.g. if needed outside Disptach. */
  def to_uri = URI.create(subject.host.toURI).resolve(subject.path)
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
object / extends ImplicitRequestTerms {
  def apply(path: String) = /\ / path
}

case class Credentials(username: String, password: String)
