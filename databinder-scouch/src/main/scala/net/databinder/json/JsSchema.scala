package net.databinder.json

import java.io.InputStream

trait JsObject extends JsValue[Map[String, Any]] {
  def my(sym: Symbol) = new {
    def as(value: JsValue[_]) = {
      value.sym = Some(sym)
      value.parent = Some(JsObject.this)
      value
    }
  }
  def loc(base: Map[String, Option[Any]], sub_sym: Symbol): Option[Map[String, Option[Any]]] = 
    ((parent match {
        case Some(parent) => parent.loc(base, sym.get)
        case None => Some(base)
      }) flatMap { _(sub_sym.name) }
    ).asInstanceOf[Option[Map[String, Option[Any]]]]
}

class JsString extends JsValue[String]

trait JsValue[T] {
  var sym: Option[Symbol] = None
  var parent: Option[JsObject] = None
  
  def loc(base: Map[String, Option[Any]]): Option[T] = 
    (parent flatMap { _.loc(base, sym.get) }).asInstanceOf[Option[T]]
}



class JsStore (val base: Map[String, Option[Any]]){
  def this(stream: InputStream) = this(Json parse stream)

  def apply[T](ref: JsValue[T]) = (ref loc base)
  
/*  def << [T](ref: JsValue[T])(value: Any): JsStore = 
    new JsStore(base + (ref.sym.name -> (value match {
      case opt: Option[_] => opt
      case value => Some(value)
    }))) */
}