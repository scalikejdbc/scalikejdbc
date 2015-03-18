package scalikejdbc.mapper

import sbt.{ InputKey, TaskKey }

object SbtKeys {

  lazy val scalikejdbcGen = InputKey[Unit]("scalikejdbc-gen", "Generates a model for a specified table")
  lazy val scalikejdbcGenForce = InputKey[Unit]("scalikejdbc-gen-force", "Generates and overwrites a model for a specified table")
  lazy val scalikejdbcGenAll = InputKey[Unit]("scalikejdbc-gen-all", "Generates models for all tables")
  lazy val scalikejdbcGenAllForce = InputKey[Unit]("scalikejdbc-gen-all-force", "Generates and overwrites models for all tables")
  lazy val scalikejdbcGenEcho = InputKey[Unit]("scalikejdbc-gen-echo", "Prints a model for a specified table")

  lazy val scalikejdbcJDBCSettings = TaskKey[SbtPlugin.JDBCSettings]("scalikejdbcJDBCSettings")
  lazy val scalikejdbcGeneratorSettings = TaskKey[SbtPlugin.GeneratorSettings]("scalikejdbcGeneratorSettings")

  val scalikejdbcCodeGeneratorSingle = TaskKey[(String, Option[String], SbtPlugin.JDBCSettings, SbtPlugin.GeneratorSettings) => Option[Generator]]("scalikejdbcCodeGeneratorSingle")
  val scalikejdbcCodeGeneratorAll = TaskKey[(SbtPlugin.JDBCSettings, SbtPlugin.GeneratorSettings) => Seq[Generator]]("scalikejdbcCodeGeneratorAll")
}
