package net.databinder.dispatch.components

import java.util.Locale

import org.apache.wicket.util.convert.IConverter
import org.apache.wicket.util.convert.converters.AbstractConverter
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.model.IModel 

import org.apache.wicket._

import org.slf4j.LoggerFactory

class MarukuLabel(id: String) extends TextFormattedLabel(id, Maruku)
class TextileLabel(id: String) extends TextFormattedLabel(id, Textile)
class SmartypantsLabel(id: String) extends TextFormattedLabel(id, Smartypants)

class TextFormattedLabel(id: String, converter: TextFormatConverter) extends Label(id) {
  
  def this(id: String, format: => TextFormat) = {
    this(id, new TextFormatConverter(format))
  }

  def this(id: String, model: IModel, format: => TextFormat) = {
    this(id, format)
    setModel(model)
  }

  def this(id: String, code: Int) = {
    this(id, new TextFormatConverter(code))
  }
  
  setEscapeModelStrings(false)

  override def getConverter(cl: Class) = converter
}

abstract class TextFormat(val code: Int, val path_name: String)

case object Textile extends TextFormat(0, "redcloth")
case object Maruku extends TextFormat(1, "maruku")
case object Smartypants extends TextFormat(2, "rubypants")

class TextFormatConverter(format: => TextFormat) extends HttpPostConverter {
  def this(code: Int) = this(TextFormat.format(code))
  def path_name = format.path_name  
}

object TextFormat {
  val format = Map(
    Maruku.code -> Maruku,
    Textile.code -> Textile,
    Smartypants.code -> Smartypants
  )
}