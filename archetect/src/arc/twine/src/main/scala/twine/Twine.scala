package dispatch.twine

import oauth._
import twitter._
import net.lag.configgy._
import Configgy.config

object Twine {
  val conf = new java.io.File(System.getProperty("user.home"), ".twine.conf")
  val consumer = Consumer("MTlF5EG4YERsW4am4D8g", "PHOde5KfyqNxkR7AmGAQ3tLaaahHS1ELb8dguqPI")
  val http = new Http

  def main(args: Array[String]) {
    conf.createNewFile()
    Configgy.configure(conf.getPath)
    
    println( (args, Token(config.configMap("access").asMap)) match {
      case (Array(), Some(tok)) => "Try again when you have something to say."
      case (args, Some(tok)) => "Posted update %s" format
        http(Status.update(args mkString " ", consumer, tok))
      case _ => get_authorization(args)
    })
  }
  def get_authorization(args: Array[String]) = {
    ((args, Token(config.configMap("request").asMap)) match {
      case (Seq(verifier), Some(tok)) => 
        http(Auth.access_token(consumer, tok, verifier)) match {
          case (access_tok, a, screen_name) =>
            ("Approved, %s! It's tweetin' time." format screen_name, "access", access_tok)
        }
      case _ => 
        val tok = http(Auth.request_token(consumer))
        val auth_uri = Auth.authorize_url(tok).to_uri
        (( try {
          // use reflection so we can compile on Java 5
          val dsk = Class.forName("java.awt.Desktop")
          dsk.getMethod("browse", classOf[java.net.URI]).invoke(dsk.getMethod("getDesktop").invoke(null), auth_uri)
          "Accept the authorization request in your browser, for the fun to begin."
        } catch {
          case _ => "Open the following URL in a browser to permit this application to tweet 4 u:\n%s".
                      format(auth_uri.toString)
        }) + "\n\nThen run `java -jar twine.jar <pin>` to complete authorization.",
          "request", tok)
    }) match {
      case (message, name, tok) =>
        val conf_writer = new java.io.FileWriter(conf)
        conf_writer write (
          """
            |<%s>
            |  oauth_token = "%s"
            |  oauth_token_secret = "%s"
            |</%s>""".stripMargin format (name, tok.value, tok.secret, name)
        )
        conf_writer.close
        message
    }
  }
}