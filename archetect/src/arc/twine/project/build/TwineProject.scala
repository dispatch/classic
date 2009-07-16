import sbt._

class TwineProject(info: ProjectInfo) extends DefaultProject(info) with extract.BasicSelfExtractingProject
{
  val dispatch = "net.databinder" %% "dispatch-twitter" % "{{dispatch.version}}"
}
