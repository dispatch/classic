package dispatch.s3

import dispatch._

object S3 {
  import javax.xml.bind.DatatypeConverter.{printBase64Binary=>encodeBase64}
  import javax.crypto

  import java.util.{Date,Locale,SimpleTimeZone}
  import java.text.SimpleDateFormat

  val UTF_8 = "UTF-8"

  object rfc822DateParser extends SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US) {
    this.setTimeZone(new SimpleTimeZone(0, "GMT"))
  }

  def trim(s: String): String = s.dropWhile(_ == ' ').reverse.dropWhile(_ == ' ').reverse.toString

  def sign(method: String, path: String, secretKey: String, date: Date,
           contentType: Option[String], contentMd5: Option[String], amzHeaders: Map[String,Set[String]]) = {
    val SHA1 = "HmacSHA1"
    val amzString = amzHeaders.toList.sortWith(_._1.toLowerCase < _._1.toLowerCase).map{ case (k,v) => "%s:%s".format(k.toLowerCase, v.map(trim _).mkString(",")) }
    val message = (method :: contentMd5.getOrElse("") :: contentType.getOrElse("") :: rfc822DateParser.format(date) :: Nil) ++ amzString ++ List(path) mkString("\n")
    val sig = {
      val mac = crypto.Mac.getInstance(SHA1)
      val key = new crypto.spec.SecretKeySpec(bytes(secretKey), SHA1)
      mac.init(key)
      new String(encodeBase64(mac.doFinal(bytes(message))))
    }
    sig
  }

  def bytes(s: String) = s.getBytes(UTF_8)

  implicit def Request2S3RequestSigner(r: Request) = new S3RequestSigner(r)
  implicit def Request2S3RequestSigner(r: String) = new S3RequestSigner(new Request(r))

  class S3RequestSigner(r: Request) {
    import org.apache.http.util.EntityUtils
    import org.apache.http.entity.BufferedHttpEntity

    type EntityHolder <: org.apache.http.message.BasicHttpEntityEnclosingRequest

    private def md5(bytes: Array[Byte]) = {
      import java.security.MessageDigest

      val r = MessageDigest.getInstance("MD5")
      r.reset
      r.update(bytes)
      new String(encodeBase64(r.digest))
    }

    def <@ (accessKey: String, secretKey: String) = {
      val req = r.body.map { ent =>
        r <:< Map("Content-MD5" -> md5(EntityUtils.toByteArray(new BufferedHttpEntity(ent))))
      }.getOrElse(r)

      val path = req.to_uri.getPath
      
      val contentType = req.headers.filter {
        case (name, _) => name.toLowerCase == "content-type"
      }.headOption.map { case (_, value) => value }.orElse {
        req.body.map { _.getContentType.getValue }
      }
      val contentMd5 = req.headers.filter {
        case (name, _) => name.toLowerCase == "content-md5" 
      }.headOption.map { case (_, value) => value }
      val amzHeaders = req.headers.filter {
        case (name, _) => name.toLowerCase.startsWith("x-amz")
      }.foldLeft(Map.empty[String, Set[String]]) { case (m, (name, value)) => 
        m + (name -> (m(name) + value))
      }
      val d = new Date
      req <:< Map("Authorization" -> "AWS %s:%s".format(accessKey, sign(req.method, path, secretKey, d, contentType, contentMd5, amzHeaders)),
                  "Date" -> S3.rfc822DateParser.format(d))
    }
  }
}

class Bucket(val name: String) extends Request(:/("s3.amazonaws.com") / name) {
  // extending request is deprecated, should try another way to structure this
  // or, rethink the deprecation
  val create = this <<< ""
}
object Bucket {
  def apply(name: String) = new Bucket(name)
}
