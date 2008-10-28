package net.databinder.dispatch.components

import java.util.Locale

import org.apache.wicket.util.convert.IConverter
import org.apache.wicket.util.convert.converters.AbstractConverter
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.model.IModel 
import org.apache.wicket.util.io.Streams
import org.apache.wicket._

import org.slf4j.LoggerFactory

import net.sf.ehcache.CacheManager
import net.sf.ehcache.Ehcache
import net.sf.ehcache.Element

import net.databinder.dispatch.Http

abstract class HttpPostConverter extends AbstractConverter {

  def service = Http.host("localhost", 8180)
  def path_name: String
  
  def convertToObject(value: String, locale: Locale): Object = null
  
  def getTargetType = classOf[String]

  override def convertToString(source: Object, locale: Locale) = try {
    HttpPostConverter.cache(path_name, source.hashCode()) {
      (service("/" + path_name) << "input" -> source).as_str
    }  
  } catch { 
    case e => 
      HttpPostConverter.log.error("Error posting to server", e);
      Application.get.getConfigurationType match {
        case Application.DEVELOPMENT => 
          throw new RestartResponseAtInterceptPageException(new ConnectionErrorPage(e))
        case _ => ""
    }
  }
}

object HttpPostConverter {
	private val log = LoggerFactory.getLogger(classOf[HttpPostConverter])
	
  private def cache[T](path_name: String, key: Any)(create: => T) = {
    
    val mgr = CacheManager.getInstance()
    val name = classOf[HttpPostConverter].getName() + ":" + path_name
    val cache = mgr.getEhcache(name) match {
      case null => mgr.addCache(name); mgr.getEhcache(name)
      case c => c
    }
    cache.get(key) match {
      case null =>
        val obj = create;
        cache put new Element(key, obj)
        obj
      case elem => elem.getValue.asInstanceOf[T]
    }
  }
}

