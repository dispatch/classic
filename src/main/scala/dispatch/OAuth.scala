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

  private def %% (s: Seq[String]) = s map % mkString "&"
  private def bytes(str: String) = str.getBytes(UTF_8)
  
  class RequestSigner(r: Request) {
    /** Convert to a post before signing */
    val <<@ = r << IMap() <@ (_, _)
    
    /** Sign request using Post (<<) parameters and query string */
    def <@ (consumer: Consumer, token: Option[Token]) = r next {
      case before: Post =>
        r.mimic(new Post(OAuth.sign(
          before.getMethod, oauth_url(r, before), params(before) ++ before.values, consumer, token))
        )(before)
      case before =>
        val signed = OAuth.sign(before.getMethod, oauth_url(r, before), params(before), consumer, token)
        before.setURI(java.net.URI.create(oauth_url(r, before) + (Http ? signed)))
        before
    }
  }
  private def oauth_url(r: Request, req: HttpRequestBase) = r.host.getOrElse("") + req.getURI.toString.split('?')(0)

  private def params(req: HttpRequestBase): IMap[String,Any] = req.getURI.getQuery match {
      case null => IMap.empty
      case query => IMap.empty ++ query.split('&').map {
        _ split "=" match { case Seq(name, value) => name -> value }
      }
  }
}