package dispatch.mime

import org.apache.http.entity.mime.{FormBodyPart, MultipartEntity}
import org.apache.http.entity.mime.content.FileBody

import java.io.File

object Mime {
  implicit def Request2ExtendedRequest(r: Request) = new MimeRequest(r)

  class MimeRequest(r: Request) {
    def <<= (name: String, file: File) = r.next {
      case post: MultipartPost => post.add(name, file)
      case req => r.mimic(new MultipartPost)(req).add(name, file)
    }
  }
}
// Not yet supported by Dispatch OAuth
class MultipartPost extends org.apache.http.client.methods.HttpPost {
  def add(name: String, file: File) = {
    entity.addPart(name, new FileBody(file))
    this
  }
  val entity = new MultipartEntity
  this setEntity entity
}
