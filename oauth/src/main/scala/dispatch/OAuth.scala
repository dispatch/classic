package dispatch.oauth
import dispatch._
import Request.{encode_%, decode_%}

import collection.Map
import collection.immutable.{TreeMap, Map=>IMap}

import javax.crypto
import java.net.URI

import org.apache.http.protocol.HTTP.UTF_8
import javax.xml.bind.DatatypeConverter.{printBase64Binary=>encodeBase64}

case class Consumer(key: String, secret: String)
case class Token(value: String, secret: String)
object Token {
  def apply[T <: Any](m: Map[String, T]): Option[Token] = List("oauth_token", "oauth_token_secret").flatMap(m.get) match {
    case value :: secret :: Nil => Some(Token(value.toString, secret.toString))
    case _ => None
  }
}

/** Import this object's methods to add signing operators to dispatch.Request */
object OAuth {
  /** @return oauth parameter map including signature */
  def sign(method: String, url: String, user_params: Map[String, Any], consumer: Consumer, 
      token: Option[Token], verifier: Option[String], callback: Option[String]) = {
    val oauth_params = IMap(
      "oauth_consumer_key" -> consumer.key,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp" -> (System.currentTimeMillis / 1000).toString,
      "oauth_nonce" -> System.nanoTime.toString,
      "oauth_version" -> "1.0"
    ) ++ token.map { "oauth_token" -> _.value } ++ 
      verifier.map { "oauth_verifier" -> _ } ++
      callback.map { "oauth_callback" -> _ }
    
    val encoded_ordered_params = (
      new TreeMap[String, String] ++ (user_params ++ oauth_params map %%)
    ) map { case (k, v) => k + "=" + v } mkString "&"
    
    val message = %%(method :: url :: encoded_ordered_params :: Nil)
    
    val SHA1 = "HmacSHA1"
    val key_str = %%(consumer.secret :: (token map { _.secret } getOrElse "") :: Nil)
    val key = new crypto.spec.SecretKeySpec(bytes(key_str), SHA1)
    val sig = {
      val mac = crypto.Mac.getInstance(SHA1)
      mac.init(key)
      new String(encodeBase64(mac.doFinal(bytes(message))))
    }
    oauth_params + ("oauth_signature" -> sig)
  }
  
  /** Out-of-band callback code */
  val oob = "oob"
  
  /** Map with oauth_callback set to the given url */
  def callback(url: String) = IMap("oauth_callback" -> url)
  
  //normalize to OAuth percent encoding
  private def %% (str: String): String = {
    val remaps = ("+", "%20") :: ("%7E", "~") :: ("*", "%2A") :: Nil
    (encode_%(str) /: remaps) { case (str, (a, b)) => str.replace(a,b) }
  }
  private def %% (s: Seq[String]): String = s map %% mkString "&"
  private def %% (t: (String, Any)): (String, String) = (%%(t._1), %%(t._2.toString))
  
  private def bytes(str: String) = str.getBytes(UTF_8)
  
  /** Add OAuth operators to dispatch.Request */
  implicit def Request2RequestSigner(r: Request) = new RequestSigner(r)
  /** Add String conversion since Http#str2req implicit will not chain. */
  implicit def Request2RequestSigner(r: String) = new RequestSigner(new Request(r))

  class RequestSigner(r: Request) {
    @deprecated("use <@ (consumer, callback) to pass the callback in the header for a request-token request")
    def <@ (consumer: Consumer): Request = sign(consumer, None, None, None)
    /** sign a request with a callback, e.g. a request-token request */
    def <@ (consumer: Consumer, callback: String): Request = sign(consumer, None, None, Some(callback))
    /** sign a request with a consumer, token, and verifier, e.g. access-token request */
    def <@ (consumer: Consumer, token: Token, verifier: String): Request = 
      sign(consumer, Some(token), Some(verifier), None)
    /** sign a request with a consumer and a token, e.g. an OAuth-signed API request */
    def <@ (consumer: Consumer, token: Token): Request = sign(consumer, Some(token), None, None)
    
    /** add token value as a query string parameter, for user authorization redirects */
    def <<? (token: Token) =  // implicit use of <<? here causes compiler error
      (new RequestTerms(r)).<<?(("oauth_token" -> token.value) :: Nil)

    /** Sign request by reading Post (<<) and query string parameters */
    private def sign(consumer: Consumer, token: Option[Token], verifier: Option[String], callback: Option[String]) = {
      val oauth_url = r.to_uri.toString.split('?')(0)
      val query_params = split_decode(r.to_uri.getRawQuery)
      val oauth_params = OAuth.sign(r.method, oauth_url, query_params ++ (
        r.body.toList.flatMap { 
          case ent: FormEntity => ent.oauth_params
          case _ => Nil
        }
      ), consumer, token, verifier, callback)
      r <:< IMap("Authorization" -> ("OAuth " + oauth_params.map { 
        case (k, v) => (encode_%(k)) + "=\"%s\"".format(encode_%(v))
      }.mkString(",") ))
    }

    def >% [T] (block: IMap[String, String] => T) = r >- ( split_decode andThen block )
    def as_token = r >% { Token(_).getOrElse { error("Token parameters not found in given map") } }
    
    val split_decode: (String => IMap[String, String]) = {
      case null => IMap.empty
      case query => IMap.empty ++ query.trim.split('&').map { nvp =>
        nvp.split("=").map(decode_%) match {
          case Array(name) => name -> ""
          case Array(name, value) => name -> value
        }
      }
    }
  }
  
}
