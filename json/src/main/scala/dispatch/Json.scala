package dispatch.json

// http://paste.pocoo.org/raw/106688/

import scala.util.parsing.combinator._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.lexical._
import scala.util.parsing.input.{Reader,StreamReader,CharArrayReader}
import java.io.{InputStream, InputStreamReader}

object JsonParser extends StdTokenParsers with ImplicitConversions {
  type Tokens = scala.util.parsing.json.Lexer
  val lexical = new Tokens
  
  lexical.reserved ++= List("true", "false", "null")
  lexical.delimiters ++= List("{", "}", "[", "]", ":", ",")
  
  def jsonObj = "{" ~> repsep(objPair, ",") <~ "}" ^^ (JsObject.apply _)
  def jsonArr = "[" ~> repsep(jsonVal, ",") <~ "]" ^^ (JsArray.apply _)
  def objPair  = jsonStr ~ (":" ~> jsonVal) ^^ { case x ~ y => (x, y) }
  def jsonVal: Parser[JsValue] =
    (jsonObj | jsonArr | jsonStr | jsonNum | "true" ^^^ JsTrue | "false" ^^^ JsFalse | "null" ^^^ JsNull)
  def jsonStr = accept("string", { case lexical.StringLit(n) => JsString(n)})
  def jsonNum = accept("number", { case lexical.NumericLit(n) => JsNumber(n) })

  def apply(input: Reader[Char]): JsValue =
    phrase(jsonVal)(new lexical.Scanner(input)) match {
      case Success(result, _) => result
      case _ => throw new Exception("Illegal JSON format")
    }
}

sealed trait JsValue {
  type T
  def self: T
  override def toString = JsValue.toJson(this)
}

case class JsString(override val self: String) extends JsValue {
  type T = String
}

/**
 * This can also be implemented with as a Double, even though BigDecimal is
 * more loyal to the json spec.
 *  NOTE: Subtle bugs can arise, i.e.
 *    BigDecimal(3.14) != BigDecimal("3.14")
 * such are the perils of floating point arithmetic.
 */
case class JsNumber(override val self: BigDecimal) extends JsValue {
  type T = BigDecimal
}

// This can extend scala.collection.MapProxy to implement Map interface
case class JsObject(override val self: Map[JsString, JsValue]) extends JsValue {
  type T = Map[JsString, JsValue]
}

// This can extend scala.SeqProxy to implement Seq interface
case class JsArray(override val self: List[JsValue]) extends JsValue {
  type T = List[JsValue]
}

sealed abstract case class JsBoolean(b: Boolean) extends JsValue {
  type T = Boolean
  val self = b
}

case object JsTrue extends JsBoolean(true)
case object JsFalse extends JsBoolean(false)
case object JsNull extends JsValue {
  type T = Null
  val self = null
}

object JsObject {
  def apply() = new JsObject(Map())
  def apply(xs: Seq[(JsString, JsValue)]) = new JsObject(Map() ++ xs)
}

object JsNumber {
  def apply(n: Int) = new JsNumber(BigDecimal(n))
  def apply(n: Long) = new JsNumber(BigDecimal(n))
  def apply(n: Float) = new JsNumber(BigDecimal(n))
  def apply(n: Double) = new JsNumber(BigDecimal(n))
  def apply(n: BigInt) = new JsNumber(BigDecimal(n))
  def apply(n: String) = new JsNumber(BigDecimal(n))
}

object JsString {
  def apply(x: Any): JsString = x match {
    case s: Symbol => new JsString(s.name)
    case s: String => new JsString(s)
    // This is a hack needed for JsObject.
    // The other option is to throw an exception here.
    case _ => new JsString(x.toString)
  }
}

object JsValue {
  def apply(x: Any): JsValue = x match {
    case null => JsNull
    case j: JsValue => j
    case true => JsTrue
    case false => JsFalse
    case s @ (_: String | _: Symbol) => JsString(s)
    case n: Int => JsNumber(n)
    case n: Long => JsNumber(n)
    case n: Float => JsNumber(n)
    case n: Double => JsNumber(n)
    case n: BigInt => JsNumber(n)
    case n: BigDecimal => JsNumber(n)
    case xs: List[_] => JsArray(xs.map(JsValue.apply))
    case m: scala.collection.Map[_, _] => JsObject(
      Map.empty ++ (for ((key, value) <- m) yield (JsString(key), JsValue(value)))
    )
    case xs: Seq[_] => JsArray(xs.map(JsValue.apply).toList)
  }

  def fromString(s: String) = JsonParser(new CharArrayReader(s.toCharArray()))
  def fromStream(s: InputStream) = JsonParser(StreamReader(new InputStreamReader(s)))

  def toJson(x: JsValue): String = x match {
    case JsNull => "null"
    case JsBoolean(b) => b.toString
    case JsString(s) => "\"" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\"", "\\\\\"").replace("\n", "\\n") + "\""
    case JsNumber(n) => n.toString
    case JsArray(xs) => xs.map(toJson).mkString("[",", ","]")
    case JsObject(m) => m.map{case (key, value) => toJson(key) + " : " + toJson(value)}.mkString("{",", ","}")
  }
}
