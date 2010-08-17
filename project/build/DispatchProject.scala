import sbt._
import archetect.ArchetectProject

class DispatchProject(info: ProjectInfo) extends ParentProject(info) with posterous.Publish
{
  override def parallelExecution = true

  lazy val futures = project("futures", "Dispatch Futures", new DispatchModule(_))
  lazy val http = project("http", "Dispatch HTTP", new HttpProject(_), futures)
  lazy val mime = project("mime", "Dispatch Mime", new DispatchModule(_) {
    val mime = "org.apache.httpcomponents" % "httpmime" % "4.0.1"
  }, http)
  lazy val json = project("json", "Dispatch JSON", new DispatchModule(_))
  lazy val http_json = project("http+json", "Dispatch HTTP JSON", new HttpProject(_), http, json)
  lazy val lift_json = project("lift-json", "Dispatch lift-json", new DispatchModule(_) {
    val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
    val (lj_org, lj_name, lj_version) = ("net.liftweb", "lift-json", "2.0-M5")
    val lift_json =
      if (buildScalaVersion startsWith "2.7.") lj_org % lj_name % lj_version
      else lj_org %% lj_name % lj_version
  }, http)
  lazy val oauth = project("oauth", "Dispatch OAuth", new DispatchModule(_), http)
  lazy val times = project("times", "Dispatch Times", new DispatchModule(_), http, json, http_json)
  lazy val couch = project("couch", "Dispatch Couch", new DispatchModule(_), http, json, http_json)
  lazy val twitter = project("twitter", "Dispatch Twitter", new DispatchModule(_), http, json, http_json, oauth)
  lazy val meetup = project("meetup", "Dispatch Meetup", new DispatchModule(_), http, lift_json, oauth, mime)

  lazy val examples = project("examples", "Dispatch Examples", new DispatchExamples(_))
  lazy val agg = project("agg", "Databinder Dispatch", new AggregateProject(_) {
    def projects = dispatch_modules
  })
<<<<<<< HEAD
  lazy val google = project("google", "Dispatch Google", new DispatchModule(_), http)
=======
  lazy val google = project("google", "Google", new DispatchModule(_), http)
>>>>>>> upstream/master
  
  def dispatch_modules = subProjects.values.toList.flatMap {
    case dm: DispatchModule => List(dm)
    case _ => Nil
  }
  override def dependencies = dispatch_modules

  val sxr_version = "0.2.3"

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)

  class DispatchModule(info: ProjectInfo) extends DefaultProject(info) with sxr.Publish {
    val specs = "org.scala-tools.testing" % "specs" % "1.6.1" % "test->default"
  }
    
  class HttpProject(info: ProjectInfo) extends DispatchModule(info) {
    val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0.1"
    val jcip = "net.jcip" % "jcip-annotations" % "1.0" % "provided->default"
    val lag_net = "lag.net repository" at "http://www.lag.net/repo"
    val configgy_test = "net.lag" % "configgy" % "1.4" % "test->default"
  }
  
  override def extraTags = "configgy" :: Nil
  
  lazy val publishExtras = task { None } dependsOn 
    (agg.doc :: examples.publishExamples :: publishCurrentNotes :: dispatch_modules.map { _.publishSxr } : _*)

  class DispatchExamples(info: ProjectInfo) extends DefaultProject(info) with ArchetectProject {
    import Process._

    override val templateMappings = Map(
      "sbt.version" -> sbtVersion.value,
      "def.scala.version" -> defScalaVersion.value,
      "build.scala.versions" -> "2.7.6",
      "sxr.version" -> sxr_version,
      "dispatch.version" -> version
    )
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
  }
  
  abstract class AggregateProject(info: ProjectInfo) extends DefaultProject(info) {
    override def compileAction = task { None }
    override def testCompileAction = task { None }
    protected def projects: Iterable[DefaultProject]
    
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
