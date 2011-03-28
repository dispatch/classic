package dispatch

import org.apache.http.message.BasicHttpRequest
import org.apache.http.{HttpEntity,HttpHost,HttpRequest}
import org.apache.http.util.EntityUtils
import org.apache.http.entity.StringEntity
import java.net.URI

object Request extends Encoders
    with ImplicitRequestVerbs
    with ImplicitHandlerVerbs 
    with ImplicitCallbackVerbs {
  /** Dispatch's factory-default charset, utf-8 */
  val factoryCharset = org.apache.http.protocol.HTTP.UTF_8
  /** Headers lists in reverse order */
  type Headers = List[(String, String)]
  implicit def strToRequest(str: String) = new Request(str)
  def to_host(uristr: String) = {
    val uri = URI.create(uristr)
    new HttpHost(uri.getHost, uri.getPort, uri.getScheme)
  }
  def to_path(uristr: String) = {
    val uri = URI.create(uristr)
    (new URI(null, null, uri.getPath, uri.getQuery, null)).toString
  }
  def defaultCharset = factoryCharset
}

/** Request descriptor, possibly contains a host, credentials, and a list of transformation functions. */
class Request(
  val host: HttpHost, 
  val creds: Option[Credentials], 
  val method: String,
  val path: String,
  val headers: Request.Headers,
  val body: Option[HttpEntity],
  val defaultCharset: String
) extends Encoders {
  /** Construct with path or full URI. */
  def this(str: String) = {
    this(Request.to_host(str), None, "GET", Request.to_path(str), Nil, None, Request.factoryCharset)
  }
  
  /** Construct with host only. */
  def this(host: HttpHost) = this(host, None, "GET", "/", Nil, None, Request.factoryCharset)

  /** Construct as a clone, e.g. in class extends clause. */
  def this(req: Request) =
    this(req.host, req.creds, req.method, req.path, req.headers, req.body, req.defaultCharset)

  def copy(
    host: HttpHost = host, 
    creds: Option[Credentials] = creds, 
    method: String = method,
    path: String = path,
    headers: Request.Headers = headers,
    body: Option[HttpEntity] = body,
    defaultCharset: String = defaultCharset
  ) = new Request(host, creds, method, path, headers, body, defaultCharset)
}

trait Encoders {
  def defaultCharset: String

  /** @return %-encoded string for use in URLs */
  def encode_% (s: String) = java.net.URLEncoder.encode(s, defaultCharset)

  /** @return %-decoded string e.g. from query string or form body */
  def decode_% (s: String) = java.net.URLDecoder.decode(s, defaultCharset)

  def encode_base64(b: Array[Byte]) = 
    org.apache.commons.codec.binary.Base64.encodeBase64(b)

  /** @return formatted and %-encoded query string, e.g. name=value&name2=value2 */
  def form_enc (values: Traversable[(String, String)]) = {
    form_join(values.map(form_elem))
  }
  def form_elem(value: (String, String)) = encode_%(value._1) + "=" + encode_%(value._2)
  def form_join(values: Traversable[String]) = values.mkString("&")
}

trait ImplicitRequestVerbs {
  implicit def toRequestVerbs (req: Request) = new RequestVerbs(req)
  implicit def stringToRequestVerbs (str: String) = new RequestVerbs(new Request(str))
}

/** These functions create new request descriptors based off of the current one.
    Most are intended to be used as infix operators; those that don't take a parameter
    have character names to be used with dot notation, e.g. 
    :/("example.com").HEAD.secure >>> {...} */
class RequestVerbs(subject: Request) {
  
  /** Set credentials that may be used for basic or digest auth; requires a host value :/(...) upon execution. */
  def as (name: String, pass: String) = subject.copy(creds=Some(Credentials(name, pass)))

  /** Add basic auth header unconditionally to this request. Does not wait for a 401 response. */
  def as_! (name: String, pass: String) = {
    this <:< Map("Authorization" -> (
      "Basic " + new String(Request.encode_base64(
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
  def <& (req: Request) = new Request(
    if (req.host.getHostName.isEmpty) subject.host else req.host, 
    req.creds orElse subject.creds,
    req.method,
    (subject.path, req.path) match {
      case (a, "/") => a
      case (a, b) if a.endsWith("/") => a + b
      case (a, b) => a + b
    },
    req.headers ::: subject.headers,
    req.body orElse subject.body,
    if (Request.factoryCharset == req.defaultCharset) subject.defaultCharset else req.defaultCharset
  )
  
  /** Set the default character set to be used when processing the request in <<, <<<, Handler#>> and
    derived operations >~, as_str, etc. (The 'factory' default is utf-8.) */
  def >\ (charset: String) = subject.copy(defaultCharset=charset)
  
  /** Combine this request with another handler. */
  def >& [T] (other: Handler[T]) = Handler(this <& other.request, other.block)
  
  /** Append an element to this request's path, joins with '/'. (mutates request) */
  def / (path: String) = subject.copy(
    path= (subject.path, path) match {
      case (a, b) if a.endsWith("/") => a + b
      case (a, b) => a + "/" + b
    }
  )
  
  /** Add headers to this request. (mutates request) */
  def <:< (values: Map[String, String]) = subject.copy(
    headers=values.toList ::: subject.headers
  )

  /* Add a gzip acceptance header */
  def gzip = this <:< Map("Accept-Encoding" -> "gzip")

  /** PUT the given string. */
  def <<< (stringbody: String): Request = PUT.copy(
    body=Some(new RefStringEntity(stringbody, "text/plain", subject.defaultCharset))
  )
  /** PUT the given file. */
  def <<< (file: java.io.File, content_type: String) = PUT.copy(
    body=Some(new org.apache.http.entity.FileEntity(file, content_type))
  )
  /** PUT the given values as a urlencoded form */
  def <<< (values: Traversable[(String, String)]): Request = PUT.copy(
    body=Some(form_ent(values))
  )

  private class UrlEncodedFormEntity(
    val oauth_params: Traversable[(String, String)]
  ) extends RefStringEntity(
    subject.form_join(
      (subject.body.map(EntityUtils.toString).filterNot { _.isEmpty }.toSeq ++
        oauth_params.map(subject.form_elem)
      )
    ),
    "application/x-www-form-urlencoded",
    subject.defaultCharset
  ) with FormEntity {
    def add(values: Traversable[(String, String)]) = 
      new UrlEncodedFormEntity(oauth_params ++ values)
  }

  private def form_ent(values: Traversable[(String, String)]) = subject.body.map {
    case ent: FormEntity => ent.add(values)
    case ent => error("trying to add post parameters << to entity: " + ent)
  }.getOrElse(new UrlEncodedFormEntity(values))


  /** Post the given key value sequence. */
  def << (values: Traversable[(String, String)]) = {
    subject.copy(body=Some(form_ent(values)),method="POST")
  }
  /** Post the given string value, with text/plain content-type. */
  def << (stringbody: String): Request =  this << (stringbody, "text/plain")
  /** Post the given string value. */
  def << (stringbody: String, contenttype: String): Request = POST.copy(
    body=Some(new RefStringEntity(
      stringbody, contenttype, subject.defaultCharset))
  )
  
  /** Add query parameters. (mutates request) */
  def <<? (values: Traversable[(String, String)]) =
    if (values.isEmpty) subject
    else subject.copy(
      path =
        if (subject.path contains '?') subject.path + '&' + subject.form_enc(values)
        else subject.path + (
          if (values.isEmpty) "" else "?" + subject.form_enc(values)
        )
    )
  
  /** HTTP post request. */
  def POST = this << Map.empty

  /** HTTP post request. */
  def PUT = subject.copy(method="PUT")
    
  /** HTTP delete request. */
  def DELETE = subject.copy(method="DELETE")
  
  /** HTTP head request. See >:> to access headers. */
  def HEAD = subject.copy(method="HEAD")


  /** @return URI based on this request, e.g. if needed outside Disptach. */
  def to_uri = URI.create(subject.host.toURI).resolve(subject.path)
}

/** Used within dispatch for entites that have "form" name value pairs,
 *  whether form-urlencoded or multipart mime. */
trait FormEntity extends HttpEntity {
  /** Should only return values that belong in an oauth signature */
  def oauth_params: Traversable[(String, String)]
  def add(values: Traversable[(String, String)]): FormEntity
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
object / extends ImplicitRequestVerbs {
  def apply(path: String) = /\ / path
}

case class Credentials(username: String, password: String)

/** Used by client APIs to build Handler or other objects via chaining, completed implicitly.
  * @see Http#builder2product */
trait Builder[T] { def product:T }
object Builder {
  implicit def builderToProduct[T](builder: Builder[T]) = builder.product
}

/** Extension of StringEntity that keeps a reference to the string */
class RefStringEntity(val string: String, 
                      contentType: String, 
                      val charset: String) extends 
  StringEntity(string, contentType, charset)
