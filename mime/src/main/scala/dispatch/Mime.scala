package dispatch.mime
import dispatch._
import java.io.{FilterOutputStream, OutputStream}
import org.apache.http.HttpEntity
import org.apache.http.entity.HttpEntityWrapper
import org.apache.http.entity.mime.{FormBodyPart, MultipartEntity}
import org.apache.http.entity.mime.content.{FileBody, StringBody, InputStreamBody, ContentBody}

import java.io.{File, InputStream}

/** Mime module for multipart form posting. Note that when using an InputStream generator, 
  chuncked encoding will be used with no Content-Length header and the stream will be closed
  after posting. It is therefore highly recommended that your generator always return a new 
  stream instance, or a Request descriptor referencing it will fail after its first use. */
object Mime {
  /** Adds multipart operators to Request */
  implicit def Request2ExtendedRequest(r: Request) = new MimeRequest(r)

  /** Request derivative with multipart operators */
  class MimeRequest(r: Request) {
    /** Add file to multipart post, will convert other post methods to multipart */
    def << (name: String, file: File) = 
      r next add(name, new FileBody(file))
    /** Alias for <<, needed by 2.8.0.Beta1-RC1's type inferencer--may be removed if the problem is fixed */
    def <<* (name: String, file: File) = << (name, file)
    /** Add file with content-type to multipart post, will convert other post methods to multipart */
    def << (name: String, file: File, content_type: String) = 
      r next add(name, new FileBody(file, content_type))

    /** Add stream generator with content-type to multipart post, will convert other post methods to multipart */
    def << (name: String, file_name: String, stream: () => InputStream, content_type: String) = 
      r next add(name, new InputStreamBody(stream(), content_type, file_name))

    /** Add stream generator to multipart post, will convert other post methods to multipart. */
    def << (name: String, file_name: String, stream: () => InputStream) = 
      r next add(name, new InputStreamBody(stream(), file_name))
    
    private def with_mpp(block: MultipartPost => MultipartPost): Request.Xf = {
      case post: MultipartPost => block(post)
      case p: Post[_] => block(Request.mimic(new MultipartPost)(p).add(p.oauth_values))
      case req => block(Request.mimic(new MultipartPost)(req))
    }
    def add(name: String, content: => ContentBody) = with_mpp { _.add(name, content) }
    def >?> (listener: PostListener) = r next with_mpp { _.listen(listener) }
  }
  type PostListener = (Long, Long) => Unit
  trait Entity extends HttpEntity { def addPart(name: String, body: ContentBody)  }
}

class MultipartPost(entity: Mime.Entity) extends Post[MultipartPost] {
  setEntity(entity)
  /** No values in a multi-part post are included in the OAuth base string */
  override def oauth_values = Map.empty
  def this() = this(new MultipartEntity with Mime.Entity)
  def add(name: String, content: ContentBody) = {
    entity.addPart(name, content)
    this
  }
  def listen(listener: Mime.PostListener) = Request.mimic(
    new MultipartPost(new CountingMultipartEntity(entity, listener))
  )(this)
  def add(more: collection.Map[String, Any]) = {
    more.elements foreach { case (key, value) =>
      entity.addPart(key, new StringBody(value.toString))
    }
    this
  }
}

class CountingMultipartEntity(delegate: Mime.Entity, 
    listener: Mime.PostListener) extends HttpEntityWrapper(delegate) with Mime.Entity {
  def addPart(name: String, body: ContentBody) { delegate.addPart(name, body) }
  override def writeTo(out: OutputStream) {
    super.writeTo(new FilterOutputStream(out) {
      var transferred = 0L
      val total = delegate.getContentLength
      override def write(b: Int) {
        super.write(b)
        transferred += 1
        listener(transferred, total)
      }
    })
  }
}
