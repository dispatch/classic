package dispatch.s3

import dispatch._

object S3 {
  import org.apache.http.client.methods.HttpPut
  import org.apache.commons.codec.binary.Base64.encodeBase64
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
    val amzString = amzHeaders.elements.toList.sort(_._1.toLowerCase < _._1.toLowerCase).map{ case (k,v) => "%s:%s".format(k.toLowerCase, v.map(trim _).mkString(",")) }
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

  class S3RequestSigner(r: Request) extends Request(r) {
    type EntityHolder <: org.apache.http.client.methods.HttpEntityEnclosingRequestBase

    def <@ (accessKey: String, secretKey: String): Request = r next { req =>
      val path = Http.to_uri(r.host, req).getPath
      
      val contentType = req.getAllHeaders.filter(_.getName.toLowerCase == "content-type").toList match {
        case Nil => req match {
          case r: EntityHolder => Some(r.getEntity.getContentType.getValue)
          case _ => None
        }
        case head :: tail => Some(head.getValue)
      }
      val contentMd5 = req.getAllHeaders.filter(_.getName.toLowerCase == "content-md5").toList match {
        case Nil => None
        case head :: tail => Some(head.getValue)
      }
      val amzHeaders = 
        req.getAllHeaders.filter(_.getName.toLowerCase.startsWith("x-amz")).foldLeft(Map[String,Set[String]]()){
          (m, h) => m + (h.getName -> (m(h.getName) + h.getValue))
        }
      val d = new Date
      req.addHeader("Authorization", "AWS %s:%s".format(accessKey, sign(req.getMethod, path, secretKey, d, contentType, contentMd5, amzHeaders)))
      req.addHeader("Date",S3.rfc822DateParser.format(d))
      req
    }
  }
}

case class Bucket(name: String) extends Request(:/("s3.amazonaws.com") / name) {
  val create = new S3.S3RequestSigner(this) <<< ""
}
