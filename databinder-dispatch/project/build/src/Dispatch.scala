import sbt._

class Dispatch(info: ProjectInfo) extends DefaultProject(info)
{
  val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0-beta2"
  
  override def ivyXML =
    <publications>
      <artifact name="databinder-dispatch" type="jar" ext="jar"/>
    </publications>
  
}
