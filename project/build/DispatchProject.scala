import sbt._

class DispatchProject(info: ProjectInfo) extends ParentProject(info)
{
  class DispatchDefault(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins {
    override def crossScalaVersions = DispatchProject.this.crossScalaVersions
    override def useDefaultConfigurations = true
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
    
  class HttpProject(info: ProjectInfo) extends DispatchDefault(info) {
    val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0-beta2"
    val lag_net = "lag.net repository" at "http://www.lag.net/repo"
    val configgy = "net.lag" % "configgy" % "1.3" % "provided->default"
    val st = "org.scala-tools.testing" % "scalatest" % "0.9.5" % "test->default"
 
    val sxr = compilerPlugin("org.scala-tools.sxr" %% "sxr" % "0.2.1")
  }
  
  override def crossScalaVersions = Set("2.7.3", "2.7.4", "2.7.5")
  override def parallelExecution = true

  lazy val http = project("http", "Dispatch", new HttpProject(_))
  lazy val json = project("json", "Dispatch JSON", new DispatchDefault(_), http)
  lazy val oauth = project("oauth", "Dispatch OAuth", new DispatchDefault(_), http)
  lazy val times = project("times", "Dispatch NY Times", new DispatchDefault(_), json)
  lazy val couch = project("couch", "Dispatch CouchDB", new DispatchDefault(_), json)
  lazy val twitter = project("twitter", "Dispatch Twitter", new DispatchDefault(_), json, oauth)

  lazy val archetect = project("archetect", "Dispatch Archetect", new ArchetectProject(_))
  
  class ArchetectProject(info: ProjectInfo) extends DefaultProject(info) {
    import Process._
    val arcOutput = outputPath / "arc"
    val arcSource = "src" / "arc"

    lazy val archetect = task { None } dependsOn ((arcSource * "*").get.map { in =>
      val out = arcOutput / in.asFile.getName
      fileTask(out from (in ** "*")) {
        FileUtilities.clean(out, log)
        FileUtilities.sync(in, out, log) orElse {
          FileUtilities.write((out / "project" / "build" / "Project.scala").asFile, """
import sbt._

class TwineProject(info: ProjectInfo) extends DefaultProject(info) with extract.BasicSelfExtractingProject
{
  val dispatch = "net.databinder" %%%% "dispatch-twitter" %% "%s"
}
""" format version, log) orElse {
            (new java.lang.ProcessBuilder("sbt", "installer") directory out.asFile) ! log match {
              case 0 => None
              case code => Some("sbt failed on archetect project %s with code %d" format (in, code))
            }
          }
        }.toSeq.firstOption
      }
    }.toSeq : _*)
  }
}
