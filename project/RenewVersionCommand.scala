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

      if (!version.endsWith("SNAPSHOT")) {
        val sandboxBuild = FilePath("sandbox/build.sbt")
        sandboxBuild.forceWrite(sandboxBuild.readAsString()
          .replaceFirst(
            "\"org.scalikejdbc\" %% \"scalikejdbc\" % \"[^\"]+\"",
            "\"org.scalikejdbc\" %% \"scalikejdbc\" % \"" + version + "\"")
          .replaceFirst(
            "\"org.scalikejdbc\" %% \"scalikejdbc-interpolation\" % \"[^\"]+\"",
            "\"org.scalikejdbc\" %% \"scalikejdbc-interpolation\" % \"" + version + "\"")
          .replaceFirst(
            "\"org.scalikejdbc\" %% \"scalikejdbc-test\" % \"[^\"]+\"",
            "\"org.scalikejdbc\" %% \"scalikejdbc-test\" % \"" + version + "\"")
         )
         val sandboxPlugins = FilePath("sandbox/project/plugins.sbt")
         sandboxPlugins.forceWrite(sandboxPlugins.readAsString()
          .replaceFirst(
            "\"org.scalikejdbc\" %% \"scalikejdbc-mapper-generator\" % \"[^\"]+\"",
            "\"org.scalikejdbc\" %% \"scalikejdbc-mapper-generator\" % \"" + version + "\"")
        )
      }

      val scriptedBuild = FilePath("scalikejdbc-mapper-generator/src/sbt-test/scalikejdbc-mapper-generator/gen/project/Build.scala")
      scriptedBuild.forceWrite(scriptedBuild.readAsString().replaceFirst(
        "\"org.scalikejdbc\" %% \"scalikejdbc\" % \"[^\"]+\"", 
        "\"org.scalikejdbc\" %% \"scalikejdbc\" % \"" + version + "\""))

      val scriptedPlugins = FilePath("scalikejdbc-mapper-generator/src/sbt-test/scalikejdbc-mapper-generator/gen/project/plugins.sbt")    
      scriptedPlugins.forceWrite(scriptedPlugins.readAsString().replaceFirst(
        "addSbtPlugin\\(\"org.scalikejdbc\" %% \"scalikejdbc-mapper-generator\" % \"[^\"]+\"", 
        "addSbtPlugin\\(\"org.scalikejdbc\" %% \"scalikejdbc-mapper-generator\" % \"" + version + "\""))

    }
  )

}

