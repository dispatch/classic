package dispatch.mime

import org.apache.http.entity.mime.{FormBodyPart, MultipartEntity}
import org.apache.http.entity.mime.content.{FileBody, StringBody}

import java.io.File

/** Mime module for multipart form posting */
object Mime {
  /** Adds multipart operators to Request */
  implicit def Request2ExtendedRequest(r: Request) = new MimeRequest(r)

  /** Request derivative with multipart operators */
  class MimeRequest(r: Request) {
    /** Add file to multipart post, will convert other post methods to multipart */
    def << (name: String, file: File) = r next add(name, file, None)
    /** Add file with content-type to multipart post, will convert other post methods to multipart */
    def << (name: String, file: File, content_type: String) = r next add(name, file, Some(content_type))
    
    private def add(name: String, file: File, content_type: Option[String]): Request.Xf = {
      case post: MultipartPost => post.add(name, file, content_type)
      case p: Post[_] => Request.mimic(new MultipartPost)(p).add(p.values).add(name, file, content_type)
      case req => Request.mimic(new MultipartPost)(req).add(name, file, content_type)
    }
    
  }
}
// Not yet supported by Dispatch OAuth
class MultipartPost(val values: Map[String, Any], entity: MultipartEntity) extends Post[MultipartPost] {
  def this() = this(Map.empty, new MultipartEntity)
  def add(name: String, file: File, content_type: Option[String]) = {
    entity.addPart(name, 
      content_type map { new FileBody(file, _) } getOrElse { new FileBody(file) }
    )
    this
  }
  setEntity(entity)
  def add(more: collection.Map[String, Any]) = {
    more.elements foreach { case (key, value) =>
      entity.addPart(key, new StringBody(value.toString))
    }
    Request.mimic(new MultipartPost(values ++ more.elements, entity))(this)
  }
}
