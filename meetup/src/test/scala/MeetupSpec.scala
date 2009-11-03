import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class MeetupSpec extends Spec with ShouldMatchers {
  import dispatch._
  import Http._
  import json.Js._
  import meetup._
  
  describe("Group Query") {
    val http = new Http
    it("should find groups in Brooklyn") {
      val res = http(Group.query.zip(11201))
      res.isEmpty should be (false)
      res map Group.urlname forall { _.toLowerCase contains "#scala" } should be (true)
    }
  }
}
