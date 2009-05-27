import sbt._

class Dispatch(info: ProjectInfo) extends DefaultProject(info)
{
  val lag_net = "lag.net repository" at "http://www.lag.net/repo"

  val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0-beta2"
  val scala_test = "org.scala-tools.testing" % "scalatest" % "0.9.5"
  val configgy = "net.lag" % "configgy" % "1.3"

  override def managedStyle = ManagedStyle.Maven
  lazy val publishTo = Resolver.file("Databinder Repository", new java.io.File("/var/dbwww/repo"))
}
