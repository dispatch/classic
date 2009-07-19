package dispatch.twine

import oauth._
import twitter._
import net.lag.configgy._
import Configgy.config

object Twine {
  val conf = new java.io.File(System.getProperty("user.home"), ".twine.conf")
  val consumer = Consumer("lrhF8SXnl5q3gFOmzku4Gw", "PbB4Mr8pKAChWmd6AocY6gLmAKzPKaszYnXyIDQhzE")
  val http = new Http

  def main(args: Array[String]) {
    conf.createNewFile()
    Configgy.configure(conf.getPath)
    
    println( (args, Token(config.configMap("access").asMap)) match {
      case (Seq("reset"), _) => conf.delete(); "OAuth credentials deleted."
      case (Seq(), Some(tok)) => "Try again, when you have something to say?"
      case (args, Some(tok)) => "Posted update %s" format
        http(Status.update(args mkString " ", consumer, tok))
      case _ => get_authorization(args)
    })
  }
  def get_authorization(args: Array[String]) = {
    ((args, Token(config.configMap("request").asMap)) match {
      case (Seq(verifier), Some(tok)) => try {
        http(Auth.access_token(consumer, tok, verifier)) match {
          case (access_tok, _, screen_name) =>
            ("Approved! It's tweetin' time, %s." format screen_name, Some(("access", access_tok)))
        } } catch {
          case StatusCode(401, _) =>
            ("Rats! That PIN %s doesn't seem to match." format verifier, None)
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
          Some(("request", tok)))
    }) match {
      case (message, None) => message
      case (message, Some((name, tok))) =>
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
