import sbt._

class TwineProject(info: ProjectInfo) extends DefaultProject(info) with extract.BasicSelfExtractingProject
{
  val lag_net = "lag.net repository" at "http://www.lag.net/repo"
  val configgy = "net.lag" % "configgy" % "1.3" % "provided->default"
  val dispatch = "net.databinder" %% "dispatch-twitter" % "{{dispatch.version}}"
  // rm below when published ivy.xml is correct
  val dispatch_oauth = "net.databinder" %% "dispatch-oauth" % "{{dispatch.version}}"
  val dispatch_json = "net.databinder" %% "dispatch-json" % "{{dispatch.version}}"
  val dispatch_http = "net.databinder" %% "dispatch-http" % "{{dispatch.version}}"
}
