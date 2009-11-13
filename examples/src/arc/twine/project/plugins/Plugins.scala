import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
	val extract = "org.scala-tools.sbt" % "installer-plugin" % "0.2.2"
}