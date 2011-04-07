import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
  val archetect = "net.databinder" % "archetect-plugin" % "0.1.3"

  val posterous = "net.databinder" % "posterous-sbt" % "0.1.7"

  val sxr_publish = "net.databinder" % "sxr-publish" % "0.2.0"
}
