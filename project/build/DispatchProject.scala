import sbt._

class DispatchProject(info: ProjectInfo) extends ParentProject(info)
{
  override def crossScalaVersions = Set("2.7.3", "2.7.4", "2.7.5")
  override def parallelExecution = true

  lazy val http = project("http", "Dispatch HTTP", new HttpProject(_))
  lazy val json = project("json", "Dispatch JSON", new DispatchDefault(_), http)
  lazy val oauth = project("oauth", "Dispatch OAuth", new DispatchDefault(_), http)
  lazy val times = project("times", "Dispatch Times", new DispatchDefault(_), http, json)
  lazy val couch = project("couch", "Dispatch Couch", new DispatchDefault(_), http, json)
  lazy val twitter = project("twitter", "Dispatch Twitter", new DispatchDefault(_), http, json, oauth)

  class DispatchDefault(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins {
    override def crossScalaVersions = DispatchProject.this.crossScalaVersions
    override def useDefaultConfigurations = true
    override def managedStyle = ManagedStyle.Maven
    val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
    Credentials(Path.userHome / ".ivy2" / ".credentials", log)
    
    def sxrMainPath = outputPath / "classes.sxr"
    def sxrTestPath = outputPath / "test-classes.sxr"
    def sxrPublishPath = Path.fromFile("/var/dbwww/sxr") / normalizedName / version.toString
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

  lazy val archetect = project("archetect", "Dispatch Archetect", new ArchetectProject(_))
  
  val tmpl_props = Map(
    "sbt.version" -> sbtVersion.get.get,
    "dispatch.version" -> version
  )
  class ArchetectProject(info: ProjectInfo) extends DefaultProject(info) {
    import Process._
    val arcSource = "src" / "arc"
    val arcOutput = outputPath / "arc"
    override def watchPaths = super.watchPaths +++ (arcSource ** "*")

    lazy val archetect = task { None } dependsOn ( (arcSource ##).descendentsExcept("*", ".*").get.filter(!_.isDirectory).map { in =>
      val out = Path.fromString(arcOutput, in.relativePath)
      val tmpl = """(.*)\{\{(.*)\}\}(.*\n)""".r
      def template(str: String): String = str match {
        case tmpl(before, key, after) => template(before + tmpl_props(key) + after)
        case _ => str
      }
      // note: file tasks built on load; will not notice files added later
      fileTask(out from in) {
        FileUtilities.readStream(in.asFile, log) { stm =>
          FileUtilities.write(out.asFile, log) { writer =>
            io.Source.fromInputStream(stm).getLines.foreach { l =>
              writer write template(l)
            }; None
          }
        }
      }
    }.toSeq: _*)

    lazy val archetectInstaller = task { None } dependsOn ( ( (arcSource * "*").get map { proj =>
      val proj_target = arcOutput / proj.asFile.getName
      val proj_target_target = proj_target / "target"
      fileTask(proj_target_target from arcSource ** "*") {
        proj_target_target.asFile.setLastModified(System.currentTimeMillis)
        (new java.lang.ProcessBuilder("sbt", "installer") directory proj_target.asFile) ! log match {
          case 0 => None
          case code => Some("sbt failed on archetect project %s with code %d" format (proj_target, code))
        }
      } dependsOn (archetect, publishLocal)
    } ).toSeq: _*) 
  }
}
