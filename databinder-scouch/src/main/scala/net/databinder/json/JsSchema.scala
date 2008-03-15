package net.databinder.json

import java.io.InputStream


trait JsonSchema {
  def loc(base: Option[Map[Symbol, Option[Any]]], sub_sym: Symbol) = 
    (base flatMap { _(sub_sym) }).asInstanceOf[Option[Map[Symbol, Option[Any]]]]

  case class String(symbol: Symbol) extends Value[java.lang.String](symbol)

  case class Int(symbol: Symbol) extends Value[scala.Int](symbol)

  case class List[T](symbol: Symbol) extends Value[scala.List[T]](symbol)

  case class Object(symbol: Symbol) extends Value[Map[Symbol, Option[Any]]](symbol) with JsonSchema {
    override def loc(base: Option[Map[Symbol, Option[Any]]], sub_sym: Symbol) = 
      super.loc(loc(base), sub_sym)
  }

  class Value[T](val sym: Symbol) {
    def loc(base: Option[Map[Symbol, Option[Any]]]) = 
      JsonSchema.this.loc(base, sym).asInstanceOf[Option[T]]
  }
}



class JsStore (val base: Map[Symbol, Option[Any]]){
  def this(stream: InputStream) = this(Json parse stream)

  def apply[T](ref: JsonSchema#Value[T]) = (ref loc Some(base))
  
/*  def << [T](ref: JsValue[T])(value: Any): JsStore = 
    new JsStore(base + (ref.sym.name -> (value match {
      case opt: Option[_] => opt
      case value => Some(value)
    }))) */
}