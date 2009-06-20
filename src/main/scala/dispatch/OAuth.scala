package dispatch.oauth
import Http.{%, q_str}

import collection.Map
import collection.immutable.{TreeMap, Map=>IMap}

import javax.crypto

import org.apache.http.protocol.HTTP.UTF_8
import org.apache.commons.codec.binary.Base64.encodeBase64
import org.apache.http.client.methods.HttpRequestBase

case class Consumer(key: String, secret: String)
case class Token(value: String, secret: String)

/** Import this object's methods to add signing operators to dispatch.Request */
object OAuth {
  implicit def Requst2RequestSigner(r: Request) = new RequestSigner(r)
  
  def sign(method: String, url: String, user_params: Map[String, Any], consumer: Consumer, token: Option[Token]) = {
    val params = TreeMap(
      "oauth_consumer_key" -> consumer.key,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_version" -> "1.0",
      "oauth_timestamp" -> (System.currentTimeMillis / 1000).toString,
      "oauth_nonce" -> System.nanoTime.toString
    ) ++ token.map { "oauth_token" -> _.value } ++ user_params
    
    val message = %%(method :: url :: q_str(params) :: Nil)
    
    val SHA1 = "HmacSHA1";
    val key_str = %%(consumer.secret :: (token map { _.secret } getOrElse "") :: Nil)
    val key = new crypto.spec.SecretKeySpec(key_str.getBytes(UTF_8), SHA1)
    val sig = {
      val mac = crypto.Mac.getInstance(SHA1)
      mac.init(key)
      new String(encodeBase64(mac.doFinal(bytes(message))))
    }
    params + ("oauth_signature" -> sig)
  }
  
  def authorize_url(url: String, consumer: Consumer, token: Token) =
    url + (Http ? sign("GET", url, IMap(), consumer, Some(token)))

  private def %% (s: Seq[String]) = s map % mkString "&"
  private def bytes(str: String) = str.getBytes(UTF_8)
  
  class RequestSigner(r: Request) {
    /** Convert to a post before signing */
    def <<@ (consumer: Consumer): Request = r << IMap() <@ (consumer)
    def <<@ (consumer: Consumer, token: Token): Request = r << IMap() <@ (consumer, token)

    def <@ (consumer: Consumer): Request = sign(consumer, None)
    def <@ (consumer: Consumer, token: Token): Request = sign(consumer, Some(token))
    
    /** Sign request using Post (<<) parameters and query string */
    private def sign (consumer: Consumer, token: Option[Token]) = r next { req =>
      val oauth_url = Http.to_uri(r.host, req).toString.split('?')(0)
      val query_map = split_decode(req.getURI.getRawQuery)
      req match {
        case before: Post =>
          r.mimic(new Post(OAuth.sign(
            before.getMethod, oauth_url, query_map ++ before.values, consumer, token))
          )(before)
        case before =>
          val signed = OAuth.sign(before.getMethod, oauth_url, query_map, consumer, token)
          before.setURI(java.net.URI.create(oauth_url + (Http ? signed)))
          before
      }
    }
    def >% [T] (block: IMap[String, String] => T) = r >- ( split_decode andThen block )
    def as_token = r >% { m => Token(m("oauth_token"), m("oauth_token_secret")) }
    
    val split_decode: (String => IMap[String, String]) = {
      case null => IMap.empty
      case query => IMap.empty ++ query.split('&').map { nvp =>
        ( nvp split "=" map Http.-% ) match { 
          case Seq(name, value) => name -> value
          case Seq(name) => name -> ""
        }
      }
    }
  }
  
}