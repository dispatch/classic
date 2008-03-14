package net.databinder.json

import java.io.InputStream

trait JsSchema {
  def string(sym: Symbol) = JsString(sym)
//  def obj(sym: Symbo)
}

case class JsString(sym: Symbol) extends JsValue[String](sym)  
case class JsObject(sym: Symbol) extends JsValue[Map[String, Any]](sym)

class JsValue[T](val symbol: Symbol)

class JsStore (val base: Map[String, Any]){
  def this(stream: InputStream) = this(Json parse stream)

  def apply[T](ref: JsValue[T]) = base(ref.symbol.name).asInstanceOf[Option[T]]
  
  def << [T](ref: JsValue[T])(value: Any): JsStore = 
    new JsStore(base + (ref.symbol.name -> (value match {
      case opt: Option[_] => opt
      case value => Some(value)
    })))
}