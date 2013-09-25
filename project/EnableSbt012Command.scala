import sbt._
import sbt.Keys._
import scalag._

object EnableSbt012Command extends Plugin {

  ScalagPlugin.addCommand(
    namespace = "sbt012",
    description = "Enables sbt 0.12.4",
    operation = { case ScalagInput(_, settings) => 
      FilePath("project/build.properties").forceWrite("sbt.version=0.12.4")
      val plugins = FilePath("project/plugins.sbt")
      plugins.forceWrite(plugins.readAsString().replaceFirst(
        """addSbtPlugin\("com.typesafe.play" % "sbt-plugin" % "2.2.0"\)""",
        """addSbtPlugin("play" % "sbt-plugin" % "2.1.5")"""))
    }
  )

}

