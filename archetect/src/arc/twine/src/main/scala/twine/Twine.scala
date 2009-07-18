package dispatch.twine

import oauth._
import twitter._
import net.lag.configgy._
import Configgy.config

object Twine {
  val consumer = Consumer("MTlF5EG4YERsW4am4D8g", "PHOde5KfyqNxkR7AmGAQ3tLaaahHS1ELb8dguqPI")
  val http = new Http

  def main(args: Array[String]) {
    val conf = new java.io.File(System.getProperty("user.home"), ".twine.conf")
    conf.createNewFile()
    Configgy.configure(conf.getPath)
    
    var conf_out = new java.io.PrintStream(new java.io.FileOutputStream(conf, true))
    
    println(Token(config.configMap("request").asMap) match {
      case Some(tok) => "read file"
      case _ =>
        val tok = http(Auth.request_token(consumer))
        conf_out.println("""
          |<request>
          |  oauth_token = "%s"
          |  oauth_token_secret = "%s"
          |</request>""".stripMargin format (tok.value, tok.secret)
        )
        "wrote file"
    })
    conf_out.close 
  }
}
