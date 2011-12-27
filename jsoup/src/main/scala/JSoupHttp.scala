package dispatch.jsoup

import dispatch._
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import io.Source
import xml.NodeSeq

trait ImplicitJSoupHandlers {
  implicit def requestToJSoupHandlers(req: Request) = new JSoupHandlers(req);
  implicit def stringToJSoupHandlers(str: String) = new JSoupHandlers(new Request(str));
}

object JSoupHttp extends ImplicitJSoupHandlers

class JSoupHandlers(request: Request) {
  /** Process response with JSoup html processor in block */
  def jsouped [T] (block: (Document) => T) = request >> { (stm, charset) =>
      block(Jsoup.parse(stm, charset, request.to_uri.toString))
  }
  /** Alias for verb jsouped */
  def </> [T] (block: (Document) => T) = jsouped(block)
  /** Conveniences handler for retrieving a org.jsoup.nodes.Document */
  def as_jsouped: Handler[Document] = jsouped { dom => dom }
  /** Conveniences handler for retrieving a NodeSeq */
  def as_jsoupedNodeSeq: Handler[NodeSeq] = jsouped { dom: Document => {
      xml.parsing.XhtmlParser(Source.fromString(dom.html))
    }
  }
}
