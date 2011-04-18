import sbt._
import archetect.ArchetectProject

class DispatchProject(info: ProjectInfo) 
  extends ParentProject(info) 
  with posterous.Publish
  with gh.Issues
  with IdeaProject
{
  override def parallelExecution = true

  lazy val futures = project("futures", "Dispatch Futures", new DispatchModule(_))
  lazy val core = project("core", "Dispatch Core", new HttpProject(_))
  lazy val d_http = project("http", "Dispatch HTTP", new HttpProject(_), core, futures)
  lazy val nio = project("nio", "Dispatch NIO", new HttpProject(_) {
    val nio_comp = "org.apache.httpcomponents" % "httpasyncclient" % "4.0-alpha1"
  }, core, futures)
  lazy val mime = project("mime", "Dispatch Mime", new DispatchModule(_) {
    val mime = "org.apache.httpcomponents" % "httpmime" % "4.1" intransitive()
    val logging = "commons-logging" % "commons-logging" % "1.1.1"
    val mime4j = "org.apache.james" % "apache-mime4j" % "0.6"
  }, core)
  lazy val json = project("json", "Dispatch JSON", new DispatchModule(_))
  lazy val http_json = project("http+json", "Dispatch HTTP JSON", new HttpProject(_), core, json)
  lazy val http_gae = project("http-gae", "Dispatch HTTP GAE", new HttpProject(_) {
    val bum_gae = "bumnetworks GAE artifacts" at "http://www.bumnetworks.com/gae"
    val gae_api = "com.google.appengine" % "appengine-api-1.0-sdk" % "1.3.6"
  }, d_http)

  lazy val lift_json = project("lift-json", "Dispatch lift-json", new DispatchModule(_) {
    val lift_json = "net.liftweb" % "lift-json_2.8.1" % "2.3"
  }, core)
  lazy val oauth = project("oauth", "Dispatch OAuth", new DispatchModule(_), core)
  lazy val times = project("times", "Dispatch Times", new DispatchModule(_), d_http, json, http_json)
  lazy val couch = project("couch", "Dispatch Couch", new DispatchModule(_), d_http, json, http_json)
  lazy val twitter = project("twitter", "Dispatch Twitter", new DispatchModule(_), core, json, http_json, oauth, lift_json, nio)
  lazy val meetup = project("meetup", "Dispatch Meetup", new DispatchModule(_), core, lift_json, oauth, mime)

  lazy val aws_s3 = project("aws-s3", "Dispatch S3", new DispatchModule(_), core)

  lazy val agg = project("agg", "Databinder Dispatch", new AggregateProject(_) {
    override def disableCrossPaths = true
    def projects = dispatch_modules
  })
  lazy val google = project("google", "Dispatch Google", new DispatchModule(_), core)
  
  def dispatch_modules = subProjects.values.toList.flatMap {
    case dm: DispatchModule => List(dm)
    case _ => Nil
  }
  override def dependencies = dispatch_modules

  val sxr_version = "0.2.3"

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Mimesis Nexus" at "http://10.101.0.202:8081/nexus/content/repositories/3rdparty/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)

  class DispatchModule(info: ProjectInfo) extends DefaultProject(info) with sxr.Publish with IdeaProject {
    val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test->default"
    override def packageSrcJar = defaultJarPath("-sources.jar")
    lazy val sourceArtifact = Artifact.sources(artifactID)
    override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageSrc)
    def testDeps = d_http :: Nil
    override def testClasspath = (super.testClasspath /: testDeps) {
      _ +++ _.projectClasspath(Configurations.Compile)
    }
    override def testCompileAction = super.testCompileAction dependsOn
      (testDeps map { _.compile } : _*)
  }
    
  class HttpProject(info: ProjectInfo) extends DispatchModule(info) {
    val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.1"
  }
  
  lazy val publishJavadocs = task { None } dependsOn agg.doc

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

  def ghCredentials = gh.LocalGhCreds(log)
  def ghRepository = ("n8han", "Databinder-Dispatch")
}
