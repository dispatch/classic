package twine

import dispatch._
import twitter._
import net.lag.configgy._

object Twine {
  def main(args: Array[String]) {
    val conf = new java.io.File(System.getProperty("user.home"), ".twine.conf")
    conf.createNewFile()
    Configgy.configure(conf.getPath)
    val http = new Http
    http(Search("#dbDispatch") >>> System.out)
  }
}
