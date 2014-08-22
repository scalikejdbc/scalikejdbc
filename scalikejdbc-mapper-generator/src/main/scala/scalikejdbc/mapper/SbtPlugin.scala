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
import scala.language.reflectiveCalls
import util.control.Exception._
import java.io.FileNotFoundException
import java.util.Locale.{ ENGLISH => en }

object SbtPlugin extends Plugin {

  import SbtKeys._

  case class JDBCSettings(driver: String, url: String, username: String, password: String, schema: String)

  case class GeneratorSettings(packageName: String, template: String, testTemplate: String, lineBreak: String, caseClassOnly: Boolean, encoding: String, autoConstruct: Boolean)

  def loadSettings(): (JDBCSettings, GeneratorSettings) = {
    val props = new java.util.Properties
    try {
      using(new java.io.FileInputStream("project/scalikejdbc-mapper-generator.properties")) {
        inputStream => props.load(inputStream)
      }
    } catch {
      case e: FileNotFoundException =>
    }
    if (props.isEmpty) {
      using(new java.io.FileInputStream("project/scalikejdbc.properties")) {
        inputStream => props.load(inputStream)
      }
    }
    (JDBCSettings(
      driver = Option(props.get("jdbc.driver")).map(_.toString).getOrElse(throw new IllegalStateException("Add jdbc.driver to project/scalikejdbc-mapper-generator.properties")),
      url = Option(props.get("jdbc.url")).map(_.toString).getOrElse(throw new IllegalStateException("Add jdbc.url to project/scalikejdbc-mapper-generator.properties")),
      username = Option(props.get("jdbc.username")).map(_.toString).getOrElse(""),
      password = Option(props.get("jdbc.password")).map(_.toString).getOrElse(""),
      schema = Option(props.get("jdbc.schema")).map(_.toString).orNull[String]
    ), GeneratorSettings(
        packageName = Option(props.get("generator.packageName")).map(_.toString).getOrElse("models"),
        template = Option(props.get("generator.template")).map(_.toString).getOrElse("executableSQL"),
        testTemplate = Option(props.get("generator.testTemplate")).map(_.toString).getOrElse("specs2unit"),
        lineBreak = Option(props.get("generator.lineBreak")).map(_.toString).getOrElse("LF"),
        caseClassOnly = Option(props.get("generator.caseClassOnly")).map(_.toString.toBoolean).getOrElse(false),
        encoding = Option(props.get("generator.encoding")).map(_.toString).getOrElse("UTF-8"),
        autoConstruct = Option(props.get("generator.autoConstruct")).map(_.toString.toBoolean).getOrElse(false)
      ))
  }

  private[this] def generatorConfig(srcDir: File, testDir: File, generatorSettings: GeneratorSettings) =
    GeneratorConfig(
      srcDir = srcDir.getAbsolutePath,
      testDir = testDir.getAbsolutePath,
      packageName = generatorSettings.packageName,
      template = GeneratorTemplate(generatorSettings.template),
      testTemplate = GeneratorTestTemplate(generatorSettings.testTemplate),
      lineBreak = LineBreak(generatorSettings.lineBreak),
      caseClassOnly = generatorSettings.caseClassOnly,
      encoding = generatorSettings.encoding,
      autoConstruct = generatorSettings.autoConstruct
    )

  private def generator(tableName: String, className: Option[String], srcDir: File, testDir: File, jdbc: JDBCSettings, generatorSettings: GeneratorSettings): Option[CodeGenerator] = {
    val config = generatorConfig(srcDir, testDir, generatorSettings)
    Class.forName(jdbc.driver) // load specified jdbc driver
    val model = Model(jdbc.url, jdbc.username, jdbc.password)
    model.table(jdbc.schema, tableName)
      .orElse(model.table(jdbc.schema, tableName.toUpperCase(en)))
      .orElse(model.table(jdbc.schema, tableName.toLowerCase(en)))
      .map { table =>
        Option(new CodeGenerator(table, className)(config))
      } getOrElse {
        println("The table is not found.")
        None
      }
  }

  def allGenerators(srcDir: File, testDir: File, jdbc: JDBCSettings, generatorSettings: GeneratorSettings): Seq[CodeGenerator] = {
    val config = generatorConfig(srcDir, testDir, generatorSettings)
    val className = None
    Class.forName(jdbc.driver) // load specified jdbc driver
    val model = Model(jdbc.url, jdbc.username, jdbc.password)
    model.allTables(jdbc.schema).map { table =>
      new CodeGenerator(table, className)(config)
    }
  }

  private final case class GenTaskParameter(table: String, clazz: Option[String])

  import complete.DefaultParsers._

  private def genTaskParser(keyName: String): complete.Parser[GenTaskParameter] = (
    Space ~> token(StringBasic, "tableName") ~ (Space ~> token(StringBasic, "(class-name)")).?
  ).map(GenTaskParameter.tupled).!!!("Usage: " + keyName + " [table-name (class-name)]")

  val scalikejdbcSettings = inConfig(Compile)(Seq(
    scalikejdbcGen := {
      val srcDir = (scalaSource in Compile).value
      val testDir = (scalaSource in Test).value
      val args = genTaskParser(scalikejdbcGen.key.label).parsed
      val gen = generator(tableName = args.table, className = args.clazz, srcDir = srcDir, testDir = testDir, jdbc = scalikejdbcJDBCSettings.value, generatorSettings = scalikejdbcGeneratorSettings.value)
      gen.foreach { g =>
        g.writeModelIfNotExist()
        g.writeSpecIfNotExist(g.specAll())
      }
    },
    scalikejdbcGenForce := {
      val srcDir = (scalaSource in Compile).value
      val testDir = (scalaSource in Test).value
      val args = genTaskParser(scalikejdbcGenForce.key.label).parsed
      val gen = generator(tableName = args.table, className = args.clazz, srcDir = srcDir, testDir = testDir, jdbc = scalikejdbcJDBCSettings.value, generatorSettings = scalikejdbcGeneratorSettings.value)
      gen.foreach { g =>
        g.writeModel()
        g.writeSpec(g.specAll())
      }
    },
    scalikejdbcGenAll := {
      val srcDir = (scalaSource in Compile).value
      val testDir = (scalaSource in Test).value
      allGenerators(srcDir, testDir, scalikejdbcJDBCSettings.value, scalikejdbcGeneratorSettings.value).foreach { g =>
        g.writeModelIfNotExist()
        g.writeSpecIfNotExist(g.specAll())
      }
    },
    scalikejdbcGenAllForce := {
      val srcDir = (scalaSource in Compile).value
      val testDir = (scalaSource in Test).value
      allGenerators(srcDir, testDir, scalikejdbcJDBCSettings.value, scalikejdbcGeneratorSettings.value).foreach { g =>
        g.writeModel()
        g.writeSpec(g.specAll())
      }
    },
    scalikejdbcGenEcho := {
      val srcDir = (scalaSource in Compile).value
      val testDir = (scalaSource in Test).value
      val args = genTaskParser(scalikejdbcGenEcho.key.label).parsed
      val gen = generator(tableName = args.table, className = args.clazz, srcDir = srcDir, testDir = testDir, jdbc = scalikejdbcJDBCSettings.value, generatorSettings = scalikejdbcGeneratorSettings.value)
      gen.foreach(g => println(g.modelAll()))
      gen.foreach(g => g.specAll().foreach(spec => println(spec)))
    },
    scalikejdbcJDBCSettings := loadSettings._1,
    scalikejdbcGeneratorSettings := loadSettings._2
  ))

  def using[R <: { def close() }, A](resource: R)(f: R => A): A = ultimately {
    ignoring(classOf[Throwable]) apply resource.close()
  } apply f(resource)

}

