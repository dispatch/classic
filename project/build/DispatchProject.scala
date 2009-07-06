import sbt._

class DispatchProject(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins
{
  val lag_net = "lag.net repository" at "http://www.lag.net/repo"

  override def useDefaultConfigurations = true
  override def crossScalaVersions = Set("2.7.3", "2.7.4", "2.7.5")
  
  val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0-beta2"
  val configgy = "net.lag" % "configgy" % "1.3" % "provided->default"

  val st = "org.scala-tools.testing" % "scalatest" % "0.9.5" % "test->default"

  val sxr = compilerPlugin("org.scala-tools.sxr" %% "sxr" % "0.2.1")
  
  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
  
  def sxrMainPath = outputPath / "classes.sxr"
  def sxrTestPath = outputPath / "test-classes.sxr"
  def sxrPublishPath = Path.fromFile("/var/dbwww/sxr") / name / projectVersion.value.toString
  lazy val publishSxr = 
    syncTask(sxrMainPath, sxrPublishPath / "main") dependsOn(
      syncTask(sxrTestPath, sxrPublishPath / "test") dependsOn(testCompile)
    )
  override def publishAction = super.publishAction dependsOn(publishSxr)
}
