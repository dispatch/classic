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
  def jsouped[T](block: (Document) => T) = request >> {
    (stm, charset) =>
      block(Jsoup.parse(stm, charset, request.to_uri.toString))
  }

  def \\> [T] (block: (Document) => T) = jsouped(block)

  def as_jsouped: Handler[Document] = jsouped { dom => dom }

  def as_jsoupedNodeSeq: Handler[NodeSeq] = jsouped { dom: Document => {
      xml.parsing.XhtmlParser(Source.fromString(dom.html))
    }
  }
}