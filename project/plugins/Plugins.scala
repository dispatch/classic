import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
  val archetect = "net.databinder" % "archetect-plugin" % "0.1.3"

  val snuggletex_repo = "snuggletex_repo" at "http://www2.ph.ed.ac.uk/maven2"
  val t_repo = "t_repo" at "http://tristanhunt.com:8081/content/groups/public/"
  val posterous = "net.databinder" % "posterous-sbt" % "0.1.6"

  val sxr_publish = "net.databinder" % "sxr-publish" % "0.2.0"

  val lessis = "less is repo" at "http://repo.lessis.me"
  val ghIssues = "me.lessis" % "sbt-gh-issues" % "0.1.0"
}
