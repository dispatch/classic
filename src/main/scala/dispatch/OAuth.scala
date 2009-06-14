package dispatch.oauth
import Http.{%, q_str}

import collection.immutable.TreeMap

import javax.crypto

import org.apache.http.protocol.HTTP.UTF_8
import org.apache.commons.codec.binary.Base64.encodeBase64

case class Consumer(key: String, secret: String)
case class Token(value: String, secret: String)

object OAuth {
  def sign(url: String, consumer: Consumer, token: Option[Token]) = {
    val params = TreeMap(
      "oauth_consumer_key" -> consumer.key,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_version" -> "1.0",
      "oauth_timestamp" -> (System.currentTimeMillis / 1000).toString,
      "oauth_nonce" -> System.nanoTime.toString
    ) ++ token.map { "oauth_token" -> _.value }
    
    val message = %%("GET" :: url :: q_str(params) :: Nil)
    println(message)
    
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

  def %% (s: Seq[String]) = s map % mkString "&"
  def bytes(str: String) = str.getBytes(UTF_8)
}