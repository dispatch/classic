package net.databinder.json

import java.io.InputStream

class JsObject(symbol: Option[Symbol], parent: Option[JsObject]) extends JsValue[Map[String, Any]](symbol, parent) {
  def this() = this(None, None)
  def this(symbol: Symbol)(implicit parent: JsObject) = this (Some(symbol), Some(parent))
  def loc(base: Map[String, Option[Any]], sub_sym: Symbol): Option[Map[String, Option[Any]]] = 
    ((par match {
        case Some(par) => par.loc(base, sym.get)
        case None => Some(base)
      }) flatMap { _(sub_sym.name) }
    ).asInstanceOf[Option[Map[String, Option[Any]]]]
    implicit val local = this
}

case class JsString(symbol: Symbol)(implicit parent: JsObject) extends JsValue[String](Some(symbol), Some(parent))

class JsValue[T](val sym: Option[Symbol], val par: Option[JsObject]) {
  def loc(base: Map[String, Option[Any]]): Option[T] = 
    (par flatMap { _.loc(base, sym.get) }).asInstanceOf[Option[T]]
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