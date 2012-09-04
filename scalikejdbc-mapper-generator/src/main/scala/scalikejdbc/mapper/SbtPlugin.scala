/*
 * Copyright 2012 Kazuhiro Sera
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

import sbt._
import sbt.Keys._
import util.control.Exception._

object SbtPlugin extends Plugin {

  import SbtKeys._

  case class JDBCSettings(driver: String, url: String, username: String, password: String, schema: String)

  case class GeneratorSetings(packageName: String, template: String, lineBreak: String, encoding: String)

  def loadSettings(): (JDBCSettings, GeneratorSetings) = {
    val props = new java.util.Properties
    using(new java.io.FileInputStream("project/scalikejdbc-mapper-generator.properties")) {
      inputStream => props.load(inputStream)
    }
    (JDBCSettings(
      driver = Option(props.get("jdbc.driver")).map(_.toString).getOrElse(throw new IllegalStateException("Add jdbc.driver to project/scalikejdbc-mapper-generator.properties")),
      url = Option(props.get("jdbc.url")).map(_.toString).getOrElse(throw new IllegalStateException("Add jdbc.url to project/scalikejdbc-mapper-generator.properties")),
      username = Option(props.get("jdbc.username")).map(_.toString).getOrElse(""),
      password = Option(props.get("jdbc.password")).map(_.toString).getOrElse(""),
      schema = Option(props.get("jdbc.schema")).map(_.toString).orNull[String]
    ), GeneratorSetings(
        packageName = Option(props.get("generator.packageName")).map(_.toString).getOrElse("models"),
        template = Option(props.get("generator.template")).map(_.toString).getOrElse("executableSQL"),
        lineBreak = Option(props.get("generator.lineBreak")).map(_.toString).getOrElse("LF"),
        encoding = Option(props.get("generator.encoding")).map(_.toString).getOrElse("UTF-8")
      ))
  }

  def generator(tableName: String, className: Option[String] = None, srcDir: File, testDir: File): Option[ARLikeTemplateGenerator] = {
    val (jdbc, generatorSettings) = loadSettings()
    Class.forName(jdbc.driver) // load specified jdbc driver
    val model = Model(jdbc.url, jdbc.username, jdbc.password)
    model.table(jdbc.schema, tableName)
      .orElse(model.table(jdbc.schema, tableName.toUpperCase))
      .orElse(model.table(jdbc.schema, tableName.toLowerCase))
      .map { table =>
        Option(ARLikeTemplateGenerator(table, className)(GeneratorConfig(
          srcDir = srcDir.getAbsolutePath,
          testDir = testDir.getAbsolutePath,
          packageName = generatorSettings.packageName,
          template = GeneratorTemplate(generatorSettings.template),
          lineBreak = LineBreak(generatorSettings.lineBreak),
          encoding = generatorSettings.encoding
        )))
      } getOrElse {
        println("The table is not found.")
        None
      }
  }

  val genTask = inputTask {
    (task: TaskKey[Seq[String]]) =>
      (task, scalaSource in Compile, scalaSource in Test) map {
        case (args, srcDir, testDir) =>
          args match {
            case Nil => println("Usage: scalikejdbc-gen [table-name (class-name)]")
            case tableName :: Nil =>
              val gen = generator(tableName = tableName, srcDir = srcDir, testDir = testDir)
              gen.foreach(_.writeFileIfNotExist())
            case tableName :: className :: Nil =>
              val gen = generator(tableName = tableName, className = Some(className), srcDir = srcDir, testDir = testDir)
              gen.foreach(_.writeFileIfNotExist())
            case _ => println("Usage: scalikejdbc-gen [table-name (class-name)]")
          }
      }
  }

  val echoTask = inputTask {
    (task: TaskKey[Seq[String]]) =>
      (task, scalaSource in Compile, scalaSource in Test) map {
        case (args, srcDir, testDir) =>
          args match {
            case Nil => println("Usage: scalikejdbc-gen-echo [table-name (class-name)]")
            case tableName :: Nil =>
              val gen = generator(tableName = tableName, srcDir = srcDir, testDir = testDir)
              gen.foreach(g => println(g.generateAll()))
            case tableName :: className :: Nil =>
              val gen = generator(tableName = tableName, className = Some(className), srcDir = srcDir, testDir = testDir)
              gen.foreach(g => println(g.generateAll()))
            case _ => println("Usage: scalikejdbc-gen-echo [table-name (class-name)]")
          }
      }
  }

  val scalikejdbcSettings = inConfig(Compile)(Seq(
    scalikejdbcGen <<= genTask,
    scalikejdbcGenEcho <<= echoTask
  ))

  def using[R <: { def close() }, A](resource: R)(f: R => A): A = ultimately {
    ignoring(classOf[Throwable]) apply resource.close()
  } apply f(resource)

}

