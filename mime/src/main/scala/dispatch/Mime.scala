package dispatch.mime
import dispatch._
import java.io.{FilterOutputStream, OutputStream}
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
    
    private def add(name: String, content: => ContentBody): Request.Xf = {
      case post: MultipartPost => post.add(name, content)
      case p: Post[_] => Request.mimic(new MultipartPost)(p).add(p.values).add(name, content)
      case req => Request.mimic(new MultipartPost)(req).add(name, content)
    }
    
  }
}

private [mime] class MultipartPost(val values: Map[String, Any], entity: MultipartEntity) extends Post[MultipartPost] {
  /** No values in a multi-part post are included in the OAuth base string */
  override def oauth_values = Map.empty
  def this() = this(Map.empty, new MultipartEntity)
  def add(name: String, content: ContentBody) = {
    entity.addPart(name, content)
    this
  }
  setEntity(entity)
  def add(more: collection.Map[String, Any]) = {
    more.elements foreach { case (key, value) =>
      entity.addPart(key, new StringBody(value.toString))
    }
    Request.mimic(new MultipartPost(values ++ more.elements, entity))(this)
  }
  type ProgressListener = {
    def transferred(num: Long): Unit
  }
}

class CountingMultipartEntity(delegate: MultipartEntity, 
    listener: MultipartPost#ProgressListener) extends HttpEntityWrapper(delegate) {
  override def writeTo(out: OutputStream) {
    super.writeTo(new FilterOutputStream(out) {
      var transferred = 0L
      def wrote(len: Int) {
        transferred += len
        listener.transferred(transferred)
      }
      override def write(b: Array[Byte], off: Int, len: Int) {
        super.write(b, off, len)
        wrote(len)
      }
      override def write(b: Int) {
        super.write(b)
        wrote(1)
      }
    })
  }
}
