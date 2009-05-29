import sbt._

class Dispatch(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins
{
  val lag_net = "lag.net repository" at "http://www.lag.net/repo"

  override def useMavenConfigurations = true

  val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0-beta2" % "compile->default"
  val configgy = "net.lag" % "configgy" % "1.3" % "compile->default"

  val st = "org.scala-tools.testing" % "scalatest" % "0.9.5" % "test->default"

  val sxr = compilerPlugin("sxr" % "sxr" % "0.1")
  
  override def managedStyle = ManagedStyle.Maven
  lazy val publishTo = Resolver.file("Databinder Repository", new java.io.File("/var/dbwww/repo"))
}
