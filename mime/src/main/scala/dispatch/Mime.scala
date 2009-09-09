package dispatch.mime

object Mime {
  implicit def Request2ExtendedRequest(r: Request) = new MimeRequest(r)

  class MimeRequest(r: Request) {
  }
}