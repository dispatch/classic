import sbt._

class TwineProject(info: ProjectInfo) extends DefaultProject(info) 
  with extract.BasicSelfExtractingProject
  with AutoCompilerPlugins
{
  val lag_net = "lag.net repository" at "http://www.lag.net/repo"
  val snapshots = "scala-tools snapshots" at "http://scala-tools.org/repo-snapshots/"
  
  val configgy = "net.lag" % "configgy" % "1.3" % "provided->default"
  val dispatch = "net.databinder" %% "dispatch-twitter" % "{{dispatch.version}}"
  // rm below when published ivy.xml is correct
  val dispatch_oauth = "net.databinder" %% "dispatch-oauth" % "{{dispatch.version}}"
  val dispatch_json = "net.databinder" %% "dispatch-json" % "{{dispatch.version}}"
  val dispatch_http = "net.databinder" %% "dispatch-http" % "{{dispatch.version}}"
  
  override def installActions = update.name :: script.name :: readme.name :: Nil
  
  val sxr = compilerPlugin("org.scala-tools.sxr" %% "sxr" % "0.2.1")
	
  // will use proguard to make one runnable jar later, for now a crazy long classpath will do
  lazy val script = task {
    import Process._
    import java.io.File
    val twine = (info.projectPath / "twine").asFile
    val twine_bat = (info.projectPath / "twine.bat").asFile
    if (System.getProperty("os.name").toLowerCase.indexOf("windows") < 0)
      FileUtilities.write(twine,
        "java -cp %s %s \"$@\"" format (
          Path.makeString((runClasspath +++ mainDependencies.scalaJars).get),
          getMainClass(false).get
        ), log
      ) orElse {
        ("chmod a+x " + twine) ! log
        None
      }
    else
      FileUtilities.write(twine_bat,
        "@echo off\njava -cp \"%s\" %s %%*" format (
          Path.makeString((runClasspath +++ mainDependencies.scalaJars).get),
          getMainClass(false).get
        ), log
      )
  } dependsOn compile
  
  lazy val readme = task {
    val rf = path("README").asFile
    print("Printing %s ==>\n\n" format rf)
    FileUtilities.readString(rf, log) fold (
      Some(_), { str => print(str); None }
    )
  }
}
