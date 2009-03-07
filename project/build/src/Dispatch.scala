import sbt._

class Dispatch(info: ProjectInfo) extends DefaultProject(info)
{
  val scala_tools = "Scala Tools Releases" at "http://scala-tools.org/repo-releases"

  val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0-beta2"
  val scala_test = "org.scala-tools.testing" % "scalatest" % "0.9.5"
  
  override def ivyXML =
    <publications>
      <artifact name="dispatch" type="jar" ext="jar"/>
    </publications>
  
}
