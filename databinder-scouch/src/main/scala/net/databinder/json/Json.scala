package net.databinder.json

import scala.util.parsing.json.Parser
import scala.util.parsing.input.{Reader,StreamReader}
import java.io.{InputStream, InputStreamReader}

object Json extends Parser {
  def parse(input: InputStream) = 
    phrase(root)(new lexical.Scanner(StreamReader(new InputStreamReader(input)))) match {
      case Success(list: List[_], _) => mapify(list head, list tail)
      case _ => Map[String, Option[Any]]()
    }

  def mapify(tup: Any, list: List[Any]): Map[String, Option[Any]] =
    (list match {
      case Nil => Map[String, Option[Any]]()
      case _ => mapify(list head, list tail)
    }) + (tup match { case (key: String, value) => key -> resolve(value) })

  def listify(value: Any, list: List[Any]): List[Any] = 
    resolve(value) :: (list match {
      case Nil => Nil
      case list => listify(list head, list tail)
    })

  def resolve(value: Any) = value match {
    case list: List[_] => Some(list.head match {
      case tup: (_, _) => mapify(tup, (list tail))
      case value => listify(value, list tail)
    })
    case null => None
    case value => Some(value)
  }
}