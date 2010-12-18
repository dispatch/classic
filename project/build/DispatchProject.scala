import sbt._
import archetect.ArchetectProject

class DispatchProject(info: ProjectInfo) extends ParentProject(info) with posterous.Publish
{
  override def parallelExecution = true

  lazy val futures = project("futures", "Dispatch Futures", new DispatchModule(_))
  lazy val http = project("http", "Dispatch HTTP", new HttpProject(_), futures)
  lazy val nio = project("nio", "Dispatch NIO", new HttpProject(_) {
    val mime = "org.apache.httpcomponents" % "httpcore-nio" % "4.1-beta1"
  }, futures)
  lazy val mime = project("mime", "Dispatch Mime", new DispatchModule(_) {
    val mime = "org.apache.httpcomponents" % "httpmime" % "4.1-beta1"
    val mime4j = "org.apache.james" % "apache-mime4j" % "0.6"
  }, http)
  lazy val json = project("json", "Dispatch JSON", new DispatchModule(_))
  lazy val http_json = project("http+json", "Dispatch HTTP JSON", new HttpProject(_), http, json)
  lazy val http_gae = project("http-gae", "Dispatch HTTP GAE", new HttpProject(_) {
    val bum_gae = "bumnetworks GAE artifacts" at "http://www.bumnetworks.com/gae"
    val gae_api = "com.google.appengine" % "appengine-api-1.0-sdk" % "1.3.4"
  }, http)

  def clunkcompile[T](for27: T, for28: T) = if (buildScalaVersion.startsWith("2.7")) for27 else for28

  lazy val lift_json = project("lift-json", "Dispatch lift-json", new DispatchModule(_) {
    val lift_json = "net.liftweb" % ("lift-json_" + clunkcompile("2.7.7", "2.8.0")) % "2.2-M1"
  }, http)
  lazy val oauth = project("oauth", "Dispatch OAuth", new DispatchModule(_), http)
  lazy val times = project("times", "Dispatch Times", new DispatchModule(_), http, json, http_json)
  lazy val couch = project("couch", "Dispatch Couch", new DispatchModule(_), http, json, http_json)
  lazy val twitter = project("twitter", "Dispatch Twitter", new DispatchModule(_), http, json, http_json, oauth, lift_json)
  lazy val meetup = project("meetup", "Dispatch Meetup", new DispatchModule(_), http, lift_json, oauth, mime)

  lazy val aws_s3 = project("aws-s3", "Dispatch S3", new DispatchModule(_), http)

  lazy val agg = project("agg", "Databinder Dispatch", new AggregateProject(_) {
    override def disableCrossPaths = true
    def projects = dispatch_modules
  })
  lazy val google = project("google", "Dispatch Google", new DispatchModule(_), http)
  
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
    val specs = 
      clunkcompile(
        "org.scala-tools.testing" % "specs" % "1.6.2.2" % "test->default",
        "org.scala-tools.testing" % "specs_2.8.1" % "1.6.6" % "test->default")
    override def packageSrcJar = defaultJarPath("-sources.jar")
    lazy val sourceArtifact = Artifact.sources(artifactID)
    override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageSrc)
  }
    
  class HttpProject(info: ProjectInfo) extends DispatchModule(info) {
    val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.1-beta1"
    val jcip = "net.jcip" % "jcip-annotations" % "1.0" % "provided->default"
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
}
