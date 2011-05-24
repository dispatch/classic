import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
  val archetect = "net.databinder" % "archetect-plugin" % "0.1.3"

  val posterous = "net.databinder" % "posterous-sbt" % "0.1.7"

  val sxr_publish = "net.databinder" % "sxr-publish" % "0.2.0"

  val lessis = "less is repo" at "http://repo.lessis.me"
  val ghIssues = "me.lessis" % "sbt-gh-issues" % "0.1.0"

  val pf = "net.databinder" % "pamflet-plugin" % "0.1.4"

  val sbtIdeaRepo = "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
  val sbtIdea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.4.0"
}
