package net.databinder.dispatch.components

import java.util.Locale

import org.apache.wicket.util.convert.IConverter
import org.apache.wicket.util.convert.converters.AbstractConverter
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.model.IModel 

import org.apache.wicket._

import org.slf4j.LoggerFactory

class MarukuLabel(id: String) extends TextFormattedLabel(id, Maruku())
class TextileLabel(id: String) extends TextFormattedLabel(id, Textile())
class SmartypantsLabel(id: String) extends TextFormattedLabel(id, Smartypants())

class TextFormattedLabel(id: String, format: TextFormat) extends Label(id) {
  
  def this(id: String, model: IModel, format: TextFormat) = {
    this(id, format)
    setModel(model)
  }
  
  setEscapeModelStrings(false)

  val converter = new TextFormatConverter(format)
  override def getConverter(cl: Class) = converter
}

class TextFormat(val code: Int, val path_name: String)

case class Textile extends TextFormat(0, "redcloth")
case class Maruku extends TextFormat(1, "maruku")
case class Smartypants extends TextFormat(2, "rubypants")

class TextFormatConverter(format: => TextFormat) extends HttpPostConverter {
  def this(code: Int) = this(TextFormat.format(code))
  def path_name = format.path_name  
}

object TextFormat {
  val format = Map(
    Maruku().code -> Maruku(),
    Textile().code -> Textile(),
    Smartypants().code -> Smartypants(),
  )
}