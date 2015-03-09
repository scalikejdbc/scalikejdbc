/*
 * Copyright 2011 - 2015 scalikejdbc.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
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
