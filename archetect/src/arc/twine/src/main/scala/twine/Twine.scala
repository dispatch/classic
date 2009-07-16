package twine

import dispatch._
import twitter._

object Twine {
  def main(args: Array[String]) {
    val http = new Http
    http(Search("#dbDispatch") >>> System.out)
  }
}