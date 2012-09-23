import sbt._
import sbt.Keys._
import scalag._

object RenewVersionCommand extends Plugin {

  ScalagPlugin.addCommand(
    namespace = "version",
    args = Seq("version"),
    description = "Renew project version",
    operation = { case ScalagInput(version :: _, settings) => 

      val projectBuild = FilePath("project/Build.scala")
      projectBuild.forceWrite(projectBuild.readAsString().replaceFirst(
        "lazy val _version = \"[^\"]+\"", 
        "lazy val _version = \"" + version + "\""))

      val scriptedBuild = FilePath("scalikejdbc-mapper-generator/src/sbt-test/scalikejdbc-mapper-generator/gen/project/Build.scala")
      scriptedBuild.forceWrite(scriptedBuild.readAsString().replaceFirst(
        "\"com.github.seratch\" %% \"scalikejdbc\" % \"[^\"]+\"", 
        "\"com.github.seratch\" %% \"scalikejdbc\" % \"" + version + "\""))

      val scriptedPlugins = FilePath("scalikejdbc-mapper-generator/src/sbt-test/scalikejdbc-mapper-generator/gen/project/plugins.sbt")    
      scriptedPlugins.forceWrite(scriptedPlugins.readAsString().replaceFirst(
        "addSbtPlugin\\(\"com.github.seratch\" %% \"scalikejdbc-mapper-generator\" % \"[^\"]+\"", 
        "addSbtPlugin\\(\"com.github.seratch\" %% \"scalikejdbc-mapper-generator\" % \"" + version + "\""))

    }
  )

}

