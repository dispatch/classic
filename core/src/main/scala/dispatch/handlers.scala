package dispatch

import org.apache.http.{HttpResponse,HttpEntity}
import java.util.zip.GZIPInputStream
import java.io.{InputStream,OutputStream}
import scala.io.Source
import collection.immutable.{Map => IMap}

/** Request handler, contains request descriptor and a function to transform the result. */
case class Handler[T](request: Request, block: Handler.F[T]) {
  /** @return new Handler composing after with this Handler's block */
  def ~> [R](after: T => R) = Handler(request, (code, res, ent) => after(block(code,res,ent)))
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
      case None => error("""
        | Response has no HttpEntity: %s
        | If no response body is expected, use a handler such as 
        | Handlers#>| that does not require one.""".stripMargin.format(res))
    } } )
}

trait ImplicitCoreHandlers {
  implicit def toCoreHandlers (req: Request) = error("new Request(req) with CoreHandler")
}

trait CoreHandlers {
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
    val charset = error("don't have entity utils here" ) /*EntityUtils.getContentCharSet(ent) match {
      case null => request.defaultCharset
      case charset => charset
    }*/
    block(stm, charset)
  } )
  /** Handle InputStream in block, handle gzip if so encoded. */
  def >> [T] (block: InputStream => T): Handler[T] = >> { (stm, charset) => block(stm) }
  /** Handle response as a scala.io.Source, in a block. Note that Source may fail if the 
      character set it receives (determined in >>) is incorrect. To process resources
      that have incorrect charset headers, use >> ((InputStream, String) => T). */
  def >~ [T] (block: Source => T) = >> { (stm, charset) => 
    block(Source.fromInputStream(stm, charset))
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
  def >+ [A, B] (block: CoreHandlers => (Handler[A], Handler[B])) = {
    new Handler[(A,B)] ( request, { (code, res, opt_ent) =>
      val (a, b) = block(new CoreHandlers { val request = /\ })
      (a.block(code, res, opt_ent), b.block(code,res,opt_ent))
    } )
  }
  /** Chain two request handlers. First handler returns a second, which may use
      values obtained by the first. Both are run on the same request. */
  def >+> [T] (block: CoreHandlers => Handler[Handler[T]]) = {
    new Handler[T] ( request, { (code, res, opt_ent) =>
      (block(new CoreHandlers { val request = /\})).block(code, res, opt_ent).block(code, res, opt_ent)
    } )
  }
}
