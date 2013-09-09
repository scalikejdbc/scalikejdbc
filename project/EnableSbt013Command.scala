import sbt._
import sbt.Keys._
import scalag._

object EnableSbt013Command extends Plugin {

  ScalagPlugin.addCommand(
    namespace = "sbt013",
    description = "Enables sbt 0.13.0",
    operation = { case ScalagInput(_, settings) => 
      FilePath("project/build.properties").forceWrite("sbt.version=0.13.0")
      val plugins = FilePath("project/plugins.sbt")
      plugins.forceWrite(plugins.readAsString().replaceFirst(
        """addSbtPlugin\("play" % "sbt-plugin" % "2.1.3"\)""",
        """addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.0-RC1")"""))
    }
  )

}

