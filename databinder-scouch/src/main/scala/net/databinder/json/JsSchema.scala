package net.databinder.json

import java.io.InputStream

trait JsSchema {
  val store = load
  def load = Json parse stream
  def stream: InputStream
  
  def string(s: Symbol) = store(s.name).asInstanceOf[Option[String]]
  def number(s: Symbol) = store(s.name).asInstanceOf[Option[Number]]
  def list[T](s: Symbol) = store(s.name).asInstanceOf[Option[List[Option[T]]]]
}