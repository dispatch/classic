import sbt._
import Keys._

object Dispatch extends Build {
  val shared = Defaults.defaultSettings ++ Seq(
    organization := "net.databinder",
    version := "0.8.6",
    crossScalaVersions := Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1"),
    libraryDependencies <++= (scalaVersion) { sv => Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.1.2",
      sv.split("[.-]").toList match {
        case "2" :: "8" :: _ => "org.scala-tools.testing" % "specs_2.8.1" % "1.6.8" % "test"
        case "2" :: "9" :: "0" :: _ => "org.scala-tools.testing" % "specs_2.9.0-1" % "1.6.8" % "test"
        case _ => "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"
      })
    },
    publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )
  lazy val dispatch =
    Project("Dispatch", file("."), settings = shared ++ Seq(
      sources in (Compile, doc) <<= (thisProjectRef, buildStructure) flatMap aggregateTask(sources),
      dependencyClasspath in (Compile, doc) <<= (thisProjectRef, buildStructure) flatMap aggregateTask(dependencyClasspath)
    )) aggregate(
      futures, core, http, nio, mime, json, http_json, oauth)
  lazy val futures =
    Project("dispatch-futures", file("futures"), settings = shared)
  lazy val core =
    Project("dispatch-core", file("core"), settings = shared)
  lazy val http =
    Project("dispatch-http", file("http"), settings = shared) dependsOn(
      core, futures)
  lazy val gae =
    Project("dispatch-gae", file("http-gae"), settings = shared ++ Seq(
      libraryDependencies +=
        "com.google.appengine" % "appengine-api-1.0-sdk" % "1.5.5"
    )) dependsOn(http)
  lazy val nio =
    Project("dispatch-nio", file("nio"), settings = shared ++ Seq(
      libraryDependencies +=
        ("org.apache.httpcomponents" % "httpasyncclient" % "4.0-alpha1")
    )) dependsOn(core, futures)
  lazy val mime =
    Project("dispatch-mime", file("mime"), settings = shared ++ Seq(
      libraryDependencies ++= Seq(
        "org.apache.httpcomponents" % "httpmime" % "4.1.2" intransitive(),
        "commons-logging" % "commons-logging" % "1.1.1",
        "org.apache.james" % "apache-mime4j" % "0.7.1"
      )
    )) dependsOn(core)
  lazy val json =
    Project("dispatch-json", file("json"), settings = shared)
  lazy val http_json =
    Project("dispatch-http-json", file("http+json"),
      settings = shared) dependsOn(core, json)
  lazy val oauth =
    Project("dispatch-oauth", file("oauth"), settings = shared) dependsOn(
      core, http)

  def aggregateTask[T](key: TaskKey[Seq[T]])(proj: ProjectRef, struct: Load.BuildStructure) = {
    def collectProjects(op: ResolvedProject => Seq[ProjectRef])(projRef: ProjectRef, struct: Load.BuildStructure): Seq[ProjectRef] = {
      val delg = Project.getProject(projRef, struct).toSeq.flatMap(op)
      // Dependencies/aggregates might have their own dependencies/aggregates, so go recursive and do distinct.
      delg.flatMap(ref => ref +: collectProjects(op)(ref, struct)).distinct
    }
    collectProjects(_.aggregate)(proj, struct).flatMap(key in (_, Compile, doc) get struct.data).join.map(_.flatten)
  }
}
