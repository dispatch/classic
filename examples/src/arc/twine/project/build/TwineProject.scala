import sbt._
import Process._

class TwineProject(info: ProjectInfo) extends DefaultProject(info) 
  with extract.BasicSelfExtractingProject
  with AutoCompilerPlugins
{
  val lag_net = "lag.net repository" at "http://www.lag.net/repo"
  val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
  
  val configgy = "net.lag" % "configgy" % "1.4" intransitive()
  val dispatch = "net.databinder" %% "dispatch-twitter" % "{{dispatch.version}}"
  
  override def installActions = update.name :: script.name :: readme.name :: Nil
  
  val sxr = compilerPlugin("org.scala-tools.sxr" %% "sxr" % "{{sxr.version}}")
  
  override def disableCrossPaths = true
	
  // will use proguard to make one runnable jar later, for now a crazy long classpath will do
  lazy val script = task {
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
    (rf.cat !)
    None
  }
}
