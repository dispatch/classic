import sbt._
import archetect.ArchetectProject

class DispatchProject(info: ProjectInfo) extends ParentProject(info)
{
  override def parallelExecution = true

  lazy val http = project("http", "Dispatch HTTP", new HttpProject(_))
  lazy val mime = project("mime", "Dispatch Mime", new DispatchDefault(_) {
    val mime = "org.apache.httpcomponents" % "httpmime" % "4.0.1"
  }, http)
  lazy val json = project("json", "Dispatch JSON", new DispatchDefault(_))
  lazy val http_json = project("http+json", "Dispatch HTTP JSON", new HttpProject(_), http, json)
  lazy val lift_json = project("lift-json", "Dispatch lift-json", new DispatchDefault(_) {
    val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
    val lift_json = "net.liftweb" %% "lift-json" % "1.1-M8"
  }, http)
  lazy val oauth = project("oauth", "Dispatch OAuth", new DispatchDefault(_), http)
  lazy val times = project("times", "Dispatch Times", new DispatchDefault(_), http, json, http_json)
  lazy val couch = project("couch", "Dispatch Couch", new DispatchDefault(_), http, json, http_json)
  lazy val twitter = project("twitter", "Dispatch Twitter", new DispatchDefault(_), http, json, http_json, oauth)
  lazy val meetup = project("meetup", "Dispatch Meetup", new DispatchDefault(_), http, lift_json, oauth, mime)
  def dispatch_modules = http :: mime :: json :: http_json :: lift_json :: oauth :: times :: couch :: twitter :: meetup:: Nil
  lazy val agg = project("agg", "Databinder Dispatch", new AggregateProject(_) {
    def projects = dispatch_modules
  })
  
  val sxr_version = "0.2.3"

  class DispatchDefault(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins {
    override def managedStyle = ManagedStyle.Maven
    lazy val publishTo = Resolver.file("Databinder Repository", new java.io.File("/var/dbwww/repo"))
    
    val snapshots = "Scala Tools Snapshots" at "http://www.scala-tools.org/repo-snapshots/"
    val specs = "org.scala-tools.testing" % "specs" % "1.6.2-SNAPSHOT" % "test->default"
    val sxr = if (buildScalaInstance.version == "2.7.6")
      compilerPlugin("org.scala-tools.sxr" %% "sxr" % sxr_version)
    else specs // reference twice, no harm done

    def sxrMainPath = outputPath / "classes.sxr"
    def sxrTestPath = outputPath / "test-classes.sxr"
    def sxrPublishPath = Path.fromFile("/var/dbwww/sxr") / normalizedName / version.toString
    lazy val publishSxr = 
      syncTask(sxrMainPath, sxrPublishPath / "main") dependsOn(
        syncTask(sxrTestPath, sxrPublishPath / "test") dependsOn(testCompile)
      )
  }
    
  class HttpProject(info: ProjectInfo) extends DispatchDefault(info) {
    val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0.1"
    val jcip = "net.jcip" % "jcip-annotations" % "1.0" % "provided->default"
    val lag_net = "lag.net repository" at "http://www.lag.net/repo"
    val configgy_test = "net.lag" % "configgy" % "1.4" % "test->default"
  }
  
  lazy val publishExtras = task { None } dependsOn 
    (agg.doc :: examples.publishExamples :: dispatch_modules.map { _.publishSxr } : _*)
	
  // parent project should not be published
  override def publishAction = task { None }
  override def publishConfiguration = publishLocalConfiguration
  
  lazy val examples = project("examples", "Dispatch Examples", new DefaultProject(_) with ArchetectProject {
    import Process._

    override val templateMappings = Map(
      "sbt.version" -> "0.5.6",
      "scala.version" -> "2.7.6",
      "sxr.version" -> sxr_version,
      "dispatch.version" -> version
    )
    // archetect project should not be published
    override def publishAction = task { None }
    override def publishConfiguration = publishLocalConfiguration
    lazy val examplesInstaller = dynamic(examplesInstallerTasks) dependsOn (archetect, publishLocal)

    def examplesInstallerTasks = task { None } named("examples-installer-complete") dependsOn (
      ("src" / "arc" * "*").get.map { proj =>
        val proj_target = arcOutput / proj.asFile.getName
        val proj_target_target = proj_target / "target"
        fileTask(proj_target_target from arcSource ** "*") {
          proj_target_target.asFile.setLastModified(System.currentTimeMillis)
          (new java.lang.ProcessBuilder("sbt", "clean", "installer") directory proj_target.asFile) ! log match {
            case 0 => None
            case code => Some("sbt failed on archetect project %s with code %d" format (proj_target, code))
          }
        }
      }.toSeq: _*
    )
    val publishExamplesPath = Path.fromFile("/var/dbwww/dispatch-examples/")
    lazy val publishExamples = copyTask((outputPath / "arc" * "*" / "target" ##) * "*.jar", 
        publishExamplesPath) dependsOn(examplesInstaller)
  })
  
  abstract class AggregateProject(info: ProjectInfo) extends DefaultProject(info) {
    protected def projects: Iterable[DefaultProject]
    
    override def compileAction = task { None }
    override def testCompileAction = task { None }
    override def publishAction = task { None }
    
    def concatenatePaths(f: DefaultProject => PathFinder) = 
      (Path.emptyPathFinder /: projects.map(f)) { _ +++ _ }
    override def mainSources = concatenatePaths(_.mainSources)
    override def mainResources = concatenatePaths(_.mainResources)
    override def testSources = concatenatePaths(_.testSources)
    override def testResources = concatenatePaths(_.testResources)
    override def compileClasspath = concatenatePaths(_.compileClasspath)
    override def docClasspath = concatenatePaths(_.docClasspath)
  }
}
