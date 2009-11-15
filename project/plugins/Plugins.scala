import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
	val extract = "net.databinder" % "archetect-plugin" % "0.1.3"
}
