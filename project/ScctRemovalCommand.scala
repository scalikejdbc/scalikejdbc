import sbt._
import sbt.Keys._
import scalag._

object ScctRemovalCommand extends Plugin {

  ScalagPlugin.addCommand(
    namespace = "scct-removal",
    description = "Removes scct settings",
    operation = { case ScalagInput(Nil, settings) => 
      Seq("library", "config", "play-plugin", "mapper-generator-core", "test") foreach { projectName =>
        val build = FilePath("scalikejdbc-" + projectName + "/build.sbt")
        build.forceWrite(build.readAsString().replaceFirst("ScctPlugin.instrumentSettings", "//ScctPlugin.instrumentSettings"))
      }
      val build = FilePath("project/Build.scala")
      build.forceWrite(build.readAsString().replaceFirst(
        """val scctInTestScope = Seq\("reaktor" %% "scct" % "0.2-SNAPSHOT" % "test"\)""",
        """val scctInTestScope = Seq()"""))
    }
  )

}

