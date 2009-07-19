import sbt._

class TwineProject(info: ProjectInfo) extends DefaultProject(info) with extract.BasicSelfExtractingProject
{
  val lag_net = "lag.net repository" at "http://www.lag.net/repo"
  val configgy = "net.lag" % "configgy" % "1.3" % "provided->default"
  val dispatch = "net.databinder" %% "dispatch-twitter" % "{{dispatch.version}}"
  // rm below when published ivy.xml is correct
  val dispatch_oauth = "net.databinder" %% "dispatch-oauth" % "{{dispatch.version}}"
  val dispatch_json = "net.databinder" %% "dispatch-json" % "{{dispatch.version}}"
  val dispatch_http = "net.databinder" %% "dispatch-http" % "{{dispatch.version}}"
  
  // will use proguard to make one runnable jar later, for now a crazy long classpath will do
  lazy val script = task {
    import java.io.File
    val twine = (info.projectPath / "twine").asFile
    FileUtilities.write(twine,
      "java -cp %s %s \"$@\"" format (
        (Path.makeString(runClasspath.get) :: mainDependencies.scalaJars.get.toList).mkString(File.pathSeparator),
        getMainClass(false).get
      ), log) orElse {
      twine setExecutable true
      None
    }
  } dependsOn compile
}
