import sbt._

class DispatchProject(info: ProjectInfo) extends DefaultProject(info)
{
  val lag_net = "lag.net repository" at "http://www.lag.net/repo"

  override def useDefaultConfigurations = true
  override def crossScalaVersions = Set("2.7.3", "2.7.4", "2.7.5")
  
  val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0-beta2"
  val configgy = "net.lag" % "configgy" % "1.3" % "provided->default"

  val st = "org.scala-tools.testing" % "scalatest" % "0.9.5" % "test->default"

  override def managedStyle = ManagedStyle.Maven
  lazy val publishTo = Resolver.file("Databinder Repository", new java.io.File("/var/dbwww/repo"))
}
