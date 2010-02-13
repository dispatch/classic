import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
	val extract = "net.databinder" % "archetect-plugin" % "0.1.3"
	val posterous = "net.databinder" % "posterous-sbt" % "0.1.0-SNAPSHOT"
}
