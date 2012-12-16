package dispatch.classic.tagsoup

import dispatch.classic.{HandlerVerbs, Request}
import xml.parsing.NoBindingFactoryAdapter

import java.io.InputStreamReader
import org.xml.sax.InputSource
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl

trait ImplicitTagSoupHandlers {
  implicit def handlerToTagSoupHandlers(h: HandlerVerbs) = new TagSoupHandlers(h)
  implicit def requestToTagSoupHandlers(req: Request) = new TagSoupHandlers(req);
  implicit def stringToTagSoupHandlers(str: String) = new TagSoupHandlers(new Request(str));
}

object TagSoupHttp extends ImplicitTagSoupHandlers

class TagSoupHandlers(subject: HandlerVerbs) {
  lazy val parserFactory = new SAXFactoryImpl
  /** Process response with TagSoup html processor in block */
  def tagsouped [T] (block: (xml.NodeSeq) => T) = subject >> { (stm, charset) =>
      block( new NoBindingFactoryAdapter().loadXML(new InputSource(new InputStreamReader(stm, charset)), parserFactory.newSAXParser()) )
  }
  /** Alias for verb tagsouped */
  def </> [T] (block: (xml.NodeSeq) => T) = tagsouped (block)
  /** Conveniences handler for retrieving a NodeSeq */
  def as_tagsouped = tagsouped {ns => ns}
}
