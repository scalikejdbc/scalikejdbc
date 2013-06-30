import sbt._
import sbt.Keys._
import scalag._

object ScctRestorationCommand extends Plugin {

  ScalagPlugin.addCommand(
    namespace = "scct-restoration",
    description = "Restores scct settings",
    operation = { case ScalagInput(Nil, settings) => 
      Seq("library", "config", "mapper-generator-core", "test") foreach { projectName =>
        val build = FilePath("scalikejdbc-" + projectName + "/build.sbt")
        build.forceWrite(build.readAsString().replaceFirst("//ScctPlugin.instrumentSettings", "ScctPlugin.instrumentSettings"))
      }
    }
  )

}

