import sbt._
import Keys._

object Dispatch extends Build {
  val shared = Defaults.defaultSettings ++ ls.Plugin.lsSettings ++ Seq(
    organization := "net.databinder",
    version := "0.8.9",
    scalaVersion := "2.10.0",
    crossScalaVersions :=
      Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1", "2.9.1-1", "2.9.2", "2.10.0"),
    libraryDependencies <++= (scalaVersion) { sv => Seq(
      sv.split("[.-]").toList match {
        case "2" :: "8" :: _ =>
          "org.scala-tools.testing" % "specs_2.8.1" % "1.6.8" % "test"
        case "2" :: "9" :: "0" :: _ =>
          "org.scala-tools.testing" % "specs_2.9.0-1" % "1.6.8" % "test"
        case "2" :: "9" :: _ =>
          "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test"
        case _ => "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"
      })
    },
    publishMavenStyle := true,
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) 
        Some("snapshots" at nexus + "content/repositories/snapshots") 
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    homepage :=
      Some(new java.net.URL("http://dispatch-classic.databinder.net/")),
    publishArtifact in Test := false,
    licenses := Seq("LGPL v3" -> url("http://www.gnu.org/licenses/lgpl.txt")),
    pomExtra := (
      <scm>
        <url>git@github.com:dispatch/reboot.git</url>
        <connection>scm:git:git@github.com:dispatch/reboot.git</connection>
      </scm>
      <developers>
        <developer>
          <id>n8han</id>
          <name>Nathan Hamblen</name>
          <url>http://twitter.com/n8han</url>
        </developer>
      </developers>)
  )
  val httpShared = shared ++ Seq(
    libraryDependencies +=
      "org.apache.httpcomponents" % "httpclient" % "4.1.3"
  )
  lazy val dispatch =
    Project("Dispatch", file("."), settings = shared ++ Seq(
      sources in (Compile, doc) <<=
        (thisProjectRef, buildStructure) flatMap (aggregateTask(sources)),
      dependencyClasspath in (Compile, doc) <<= 
        (thisProjectRef, buildStructure) flatMap
          aggregateTask(dependencyClasspath),
      ls.Plugin.LsKeys.skipWrite := true
    )) aggregate(
      futures, core, http, nio, mime, json, http_json, oauth, gae, tagsoup, 
      jsoup
    )
  lazy val futures =
    Project("dispatch-futures", file("futures"), settings = shared ++ Seq(
      description := "Common interface to Java and Scala futures",
      // https://github.com/harrah/xsbt/issues/85#issuecomment-1687483
      unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist")),
      actorsDependency
    ))
  lazy val core =
    Project("dispatch-core", file("core"), settings = httpShared ++ Seq(
      description :=
        "Core interfaces, applied by dispatch-http and dispatch-nio executors"
    ))
  lazy val http =
    Project("dispatch-http", file("http"), settings = httpShared ++ Seq(
      description :=
        "Standard HTTP executor, uses Apache DefaultHttpClient"
    )) dependsOn(
      core, futures)
  lazy val gae =
    Project("dispatch-gae", file("http-gae"), settings = httpShared ++ Seq(
      description :=
        "Executor with a modified Apache HttpClient for Google App Engine",
      libraryDependencies +=
        "com.google.appengine" % "appengine-api-1.0-sdk" % "1.5.5"
    )) dependsOn(http)
  lazy val nio =
    Project("dispatch-nio", file("nio"), settings = httpShared ++ Seq(
      description :=
        "NIO HTTP executor, uses Apache DefaultHttpAsyncClient",
      libraryDependencies +=
        ("org.apache.httpcomponents" % "httpasyncclient" % "4.0-alpha1")
    )) dependsOn(core, futures)
  lazy val mime =
    Project("dispatch-mime", file("mime"), settings = httpShared ++ Seq(
      description :=
        "Support for multipart MIME POSTs",
      libraryDependencies ++= Seq(
        "org.apache.httpcomponents" % "httpmime" % "4.1.2" intransitive(),
        "commons-logging" % "commons-logging" % "1.1.1",
        "org.apache.james" % "apache-mime4j-core" % "0.7.2"
      ),
      actorsDependency
    )) dependsOn(core)
  lazy val json =
    Project("dispatch-json", file("json"), settings = shared ++ Seq(
      description := "A JSON parser",
      // https://github.com/harrah/xsbt/issues/85#issuecomment-1687483
      unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist"))
    ))
  lazy val http_json =
    Project("dispatch-http-json", file("http+json"),
      settings = httpShared ++ Seq(
        description := "Adds JSON handler verbs to Dispatch"
      )) dependsOn(core, json)
  lazy val oauth =
    Project("dispatch-oauth", file("oauth"), settings = httpShared ++ Seq(
      description := "OAuth 1.0a signing for Dispatch requests"
    )) dependsOn(
      core, http)
  lazy val tagsoup =
    Project("dispatch-tagsoup", file("tagsoup"), settings = httpShared ++ Seq(
      description := "Adds TagSoup handler verbs to Dispatch",
      libraryDependencies ++= Seq(
        "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
        "org.eclipse.jetty.aggregate" % "jetty-server" % "7.5.4.v20111024" % "test"
      )
    )) dependsOn(core, http)
  lazy val jsoup =
    Project("dispatch-jsoup", file("jsoup"), settings = httpShared ++ Seq(
      description := "Adds JSoup handler verbs to Dispatch",
      libraryDependencies ++= Seq(
        "org.jsoup" % "jsoup" % "1.6.1",
        "org.eclipse.jetty.aggregate" % "jetty-server" % "7.5.4.v20111024" % "test"
      )
    )) dependsOn(core, http)

  def aggregateTask[T](key: TaskKey[Seq[T]])
                      (proj: ProjectRef, struct: Load.BuildStructure) = {
    def collectProjects(op: ResolvedProject => Seq[ProjectRef])
                       (projRef: ProjectRef,
                        struct: Load.BuildStructure): Seq[ProjectRef] = {
      val delg = Project.getProject(projRef, struct).toSeq.flatMap(op)
      // Dependencies/aggregates might have their own dependencies/aggregates
      // so go recursive and do distinct.
      delg.flatMap(ref => ref +: collectProjects(op)(ref, struct)).distinct
    }
    collectProjects(_.aggregate)(proj, struct).flatMap(
      key in (_, Compile, doc) get struct.data
    ).join.map(_.flatten)
  }

  lazy val actorsDependency = libraryDependencies <<= (libraryDependencies, scalaVersion){
    (dependencies, scalaVersion) =>
      if(scalaVersion.startsWith("2.10"))
        ("org.scala-lang" % "scala-actors" % scalaVersion) +: dependencies
      else
        dependencies
    }

}
