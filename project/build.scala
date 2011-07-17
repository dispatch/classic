import sbt._
import Keys._

object Dispatch extends Build {
  val shared = Defaults.defaultSettings ++ Seq(
    organization := "net.databinder",
    version := "0.8.4-SNAPSHOT",
    crossScalaVersions := Seq("2.8.0", "2.8.1", "2.9.0", "2.9.0-1"),
    libraryDependencies +=
      ("org.apache.httpcomponents" % "httpclient" % "4.1")
  )
  lazy val dispatch =
    Project("Dispatch", file("."), settings = shared) aggregate(
      futures, core, http, nio, mime, json, http_json,
      lift_json, oauth)
  lazy val futures =
    Project("dispatch-futures", file("futures"), settings = shared)
  lazy val core =
    Project("dispatch-core", file("core"), settings = shared)
  lazy val http =
    Project("dispatch-http", file("http"), settings = shared) dependsOn(
      core, futures)
  lazy val nio =
    Project("dispatch-nio", file("nio"), settings = shared ++ Seq(
      libraryDependencies += 
        ("org.apache.httpcomponents" % "httpasyncclient" % "4.0-alpha1")
    )) dependsOn(core, futures)
  lazy val mime =
    Project("dispatch-mime", file("mime"), settings = shared ++ Seq(
      libraryDependencies ++= Seq(
        "org.apache.httpcomponents" % "httpmime" % "4.1" intransitive(),
        "commons-logging" % "commons-logging" % "1.1.1",
        "org.apache.james" % "apache-mime4j" % "0.6"
      )
    )) dependsOn(core)
  lazy val json =
    Project("dispatch-json", file("json"), settings = shared)
  lazy val http_json =
    Project("dispatch-http-json", file("http+json"),
      settings = shared) dependsOn(core, json)
  lazy val lift_json =
    Project("dispatch-lift-json", file("lift-json"), settings =
      shared ++ Seq(
        libraryDependencies += ("net.liftweb" % "lift-json_2.8.1" % "2.3")
      )
    ) dependsOn(core)
  lazy val oauth =
    Project("dispatch-oauth", file("oauth"), settings = shared) dependsOn(
      core)
}
              
                             
