// http://minformix.org/blog/index.php?/archives/91-Scala-Querying-an-objects-fields-and-methods-with-reflection.html
import scala.Console._  
import scala.tools.nsc.util.NameTransformer._  
  
class AnyExtras(x: Any) {  
  def methods = methods__.foreach(println _)  
  def fields = fields__.foreach(println _)  
    
  def methods__ = wrapped.getClass  
      .getMethods  
      .toList  
      .map(m => decode(m.toString  
                        .replaceFirst("\\).*", ")")  
                        .replaceAll("[^(]+\\.", "")  
                        .replace("()", "")))  
      .filter(!_.startsWith("$tag"))  
    
  def fields__ = wrapped.getClass  
      .getDeclaredFields  
      .toList  
      .map(m => decode(m.toString.replaceFirst("^.*\\.", "")))  
  
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

val svc = new Server("services.newsgator.com")
svc always { _.addHeader("X-NGAPIToken", token) }
svc auth (user, pass)
svc("/ngws/svc/Location.aspx") >>> System.out

/*
val couch = new Http("localhost", 5984)
object Person extends Doc {
  val name = String('name) 
  val age = Int('age)
  val pets = List[java.lang.String]('pets)
  
  val obj = new Object('objective) {
    val acc = String('accomplished)
  }
}

var p = couch("/people/nathan") >> { new Store(_) }

p = (p << Person.name)(Some("Nathan"))

p = couch("/people/nathan") <<: Revise(p)
*/