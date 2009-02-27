// http://minformix.org/blog/index.php?/archives/91-Scala-Querying-an-objects-fields-and-methods-with-reflection.html
import scala.Console._  
import scala.tools.nsc.util.NameTransformer._  

import java.util.regex._

class AnyExtras(x: Any) {  
  def methods = method_list(ig => true).foreach(println _)  
  def methods(regex: String) = method_list(has(_, regex)).foreach(println _)
  def fields = field_list(ig => true).foreach(println _)  
  def fields(regex: String) = field_list(has(_, regex)).foreach(println _)  
  
  def has(str:String, regex: String) = 
    Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(str).find
    
  def method_list(filter: String => Boolean) = wrapped.getClass  
      .getMethods  
      .toList
      .filter(m => filter(m.getName))
      .map(m => decode(m.toString  
                        .replaceFirst("\\).*", ")")  
                        .replaceAll("[^(]+\\.", "")  
                        .replace("()", "")))  
      .filter(!_.startsWith("$tag"))  
      .sort(_ < _)
    
  def field_list(filter: String => Boolean) = wrapped.getClass  
      .getDeclaredFields  
      .filter(f => filter(f.getName))
      .toList
      .map(m => decode(m.toString.replaceFirst("^.*\\.", "")))  
      .sort(_ < _)
  
  private def wrapped: AnyRef = x match {  
    case x: Byte => byte2Byte(x)  
    case x: Short => short2Short(x)  
    case x: Char => char2Character(x)  
    case x: Int => int2Integer(x)  
    case x: Long => long2Long(x)  
    case x: Float => float2Float(x)  
    case x: Double => double2Double(x)  
    case x: Boolean => boolean2Boolean(x)  
    case _ => x.asInstanceOf[AnyRef]  
  }  
}  

implicit def any2anyExtras(x: Any) = new AnyExtras(x)  

import net.databinder.dispatch._
import JsDef._
import net.databinder.dispatch.couch_

