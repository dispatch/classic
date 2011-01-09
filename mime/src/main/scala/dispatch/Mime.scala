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
  implicit def Request2ExtendedRequest(r: Request) = new MimeRequestTerms(r)

  type Headers = Map[String, List[String]]
  type MultipartBlock[T] = (Headers, InputStream) => T

  /** Request derivative with multipart operators */
  class MimeRequestTerms(r: Request) {
    import Handlers._
    /** Process parts of a multipart response in a block. The block is called once for each part
        with a Map[String,List[String]] of its headers and an InputStream of the body. */
    def >--> [T] (multipart_block: MultipartBlock[T]) = r >+> { r2 => 
      r2 >:> { headers => 
        r2 >> headers("Content-Type").find { h => true }.map(mime_stream_parser(multipart_block)).get
      }
    }

    /** Add file to multipart post, will convert other post methods to multipart */
    def << (name: String, file: File) = 
      add(name, new FileBody(file))
    /** Add file with content-type to multipart post, will convert other post methods to multipart */
    def << (name: String, file: File, content_type: String) = 
      add(name, new FileBody(file, content_type))

    /** Add stream generator with content-type to multipart post, will convert other post methods to multipart */
    def << (name: String, file_name: String, stream: () => InputStream, content_type: String) = 
      add(name, new InputStreamBody(stream(), content_type, file_name))

    /** Add stream generator to multipart post, will convert other post methods to multipart. */
    def << (name: String, file_name: String, stream: () => InputStream) = 
      add(name, new InputStreamBody(stream(), file_name))
    
    private def mime_ent: Mime.Entity = {
      r.body.map {
        case ent: Mime.Entity => ent
        case orig: FormEntity =>
          (new MultipartEntity with Mime.Entity).add(orig.oauth_params)
        case ent => error("trying to add multipart content to entity: " + ent)

      } getOrElse new MultipartEntity with Mime.Entity
    }
    def add(name: String, content: => ContentBody) = {
      val ent = mime_ent
      ent.addPart(name, content)
      r.POST.copy(body=Some(ent))
    }
    /** Add a listener function to be called as bytes are uploaded */
    def >?> (listener_f: ListenerF) = r.copy(
      body=Some(new CountingMultipartEntity(mime_ent, listener_f))
    )
  }
  /** Post listener function. Called once with the total bytes; the function returned is
    called with the bytes uploaded at each kilobyte boundary, and when complete. */
  type ListenerF = Long => Long => Unit
  trait Entity extends HttpEntity with FormEntity { 
    def addPart(name: String, body: ContentBody)
    def add(values: Iterable[(String, String)]) = {
      for ((name,value) <- values) addPart(name, new StringBody(value))
      this
    }
    def oauth_params = Nil
  }
  
  def mime_stream_parser[T](multipart_block: MultipartBlock[T])(content_type: String)(stm: InputStream) = {
    import org.apache.james.mime4j.parser.{MimeTokenStream, EntityStates}
    val m = new MimeTokenStream()
    m.parseHeadless(stm, content_type)
    val empty_headers = Map.empty[String, List[String]]
    def walk(state: Int, headers: Headers, outs: List[T]): List[T] = {
      import EntityStates._
      state match {
        case T_END_OF_STREAM => outs
        case T_FIELD =>
          val added = headers + ((m.getField.getName, 
            m.getField.getBody :: headers.getOrElse(m.getField.getName, Nil)))
            walk(m.next(), added, outs)
        case T_BODY =>
          val output = multipart_block(headers, m.getInputStream)
          walk(m.next(), empty_headers, output :: outs)
        case state =>
          walk(m.next(), headers, outs)
      }
    }
    walk(m.getState, empty_headers, Nil).reverse
  }
}

/** Byte-counting entity writer used when a listener function is passed in to the MimeRequest. */
class CountingMultipartEntity(delegate: Mime.Entity, 
    listener_f: Mime.ListenerF) extends HttpEntityWrapper(delegate) with Mime.Entity {
  def addPart(name: String, body: ContentBody) { delegate.addPart(name, body) }
  override def writeTo(out: OutputStream) {
    import scala.actors.Actor._
    super.writeTo(new FilterOutputStream(out) {
      var transferred = 0L
      val total = delegate.getContentLength
      val sent = listener_f(total)
      val listener = actor { loop { react {
        case l: Long => {
          sent(l)
          if (l == total) exit()
        }
      } } }
      override def write(b: Int) {
        super.write(b)
        transferred += 1
        if (transferred % 1024 == 0 || transferred == total)
          listener ! transferred
      }
    })
  }
}
