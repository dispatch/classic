package net.databinder.dispatch.components

import java.util.Locale

import org.apache.wicket.util.convert.IConverter
import org.apache.wicket.util.convert.converters.AbstractConverter
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.model.IModel 

import org.apache.wicket._

import org.slf4j.LoggerFactory

import net.sf.ehcache.CacheManager
import net.sf.ehcache.Ehcache
import net.sf.ehcache.Element

import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

abstract class HttpPostConverter extends AbstractConverter {

  def server_base = "http://localhost:8180/"
  def path_name: String
  
  def convertToObject(value: String, locale: Locale): Object = null
  
  def getTargetType = classOf[String]

  override def convertToString(source: Object, locale: Locale) = {
    val key = source.hashCode();
    val cache = HttpPostConverter.cache_for(path_name);
    val elem = cache.get(key);
    
    if (elem != null) 
      elem.getValue().asInstanceOf[String]
    else {
      val post_method = new PostMethod(server_base + path_name)
      try {
        post_method.setParameter("input", source.toString())
        post_method.setRequestHeader(
         "Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
      
        (new HttpClient).executeMethod(post_method) match {
          case 200 => ()
          case code => error("Bad response code: " + code)
        }
        val out = post_method.getResponseBodyAsString()
        cache.put(new Element(key, out))
        out
      } catch {
        case e =>
          if (Application.get().getConfigurationType().equals(Application.DEVELOPMENT))
            throw new RuntimeException(e)//new RestartResponseAtInterceptPageException(new ConnectionErrorPage(e))
          else {
            HttpPostConverter.log.error("Error posting to server", e)
            null
          }
      } finally {
        post_method.releaseConnection()
      }
    }
  }
}

object HttpPostConverter {
	private val log = LoggerFactory.getLogger(classOf[HttpPostConverter])
	
  private def cache_for(path_name: String) = {
    val mgr = CacheManager.getInstance()
    val name = classOf[HttpPostConverter].getName() + ":" + path_name
    val cache = mgr.getEhcache(name)
    if (cache != null)
      cache
    else {
      mgr.addCache(name)
      mgr.getEhcache(name)
    }
  }
}

