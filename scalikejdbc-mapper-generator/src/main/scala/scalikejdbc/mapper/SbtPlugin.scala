package scalikejdbc.mapper

import sbt._
import sbt.Keys._
import sbt.complete.EditDistance
import scala.language.reflectiveCalls
import util.control.Exception._
import java.io.FileNotFoundException
import java.util.Locale.{ ENGLISH => en }
import java.util.Properties

object SbtPlugin extends Plugin {

  import SbtKeys._

  case class JDBCSettings(driver: String, url: String, username: String, password: String, schema: String)

  case class GeneratorSettings(
    packageName: String,
    template: String,
    testTemplate: String,
    lineBreak: String,
    caseClassOnly: Boolean,
    encoding: String,
    autoConstruct: Boolean,
    defaultAutoSession: Boolean,
    dateTimeClass: DateTimeClass,
    tableNameToClassName: String => String,
    columnNameToFieldName: String => String,
    returnCollectionType: ReturnCollectionType,
    view: Boolean
  )

  private[this] def getString(props: Properties, key: String): Option[String] =
    Option(props.get(key)).map { value =>
      val str = value.toString
      if (str.startsWith("\"") && str.endsWith("\"") && str.length >= 2) {
        str.substring(1, str.length - 1)
      } else str
    }

  private[this] final val JDBC = "jdbc."
  private[this] final val JDBC_DRIVER = JDBC + "driver"
  private[this] final val JDBC_URL = JDBC + "url"
  private[this] final val JDBC_USER_NAME = JDBC + "username"
  private[this] final val JDBC_PASSWORD = JDBC + "password"
  private[this] final val JDBC_SCHEMA = JDBC + "schema"

  private[this] final val GENERATOR = "generator."
  private[this] final val PACKAGE_NAME = GENERATOR + "packageName"
  private[this] final val TEMPLATE = GENERATOR + "template"
  private[this] final val TEST_TEMPLATE = GENERATOR + "testTemplate"
  private[this] final val LINE_BREAK = GENERATOR + "lineBreak"
  private[this] final val CASE_CLASS_ONLY = GENERATOR + "caseClassOnly"
  private[this] final val ENCODING = GENERATOR + "encoding"
  private[this] final val AUTO_CONSTRUCT = GENERATOR + "autoConstruct"
  private[this] final val DEFAULT_AUTO_SESSION = GENERATOR + "defaultAutoSession"
  private[this] final val DATETIME_CLASS = GENERATOR + "dateTimeClass"
  private[this] final val RETURN_COLLECTION_TYPE = GENERATOR + "returnCollectionType"
  private[this] final val VIEW = GENERATOR + "view"

  private[this] val jdbcKeys = Set(
    JDBC_DRIVER, JDBC_URL, JDBC_USER_NAME, JDBC_PASSWORD, JDBC_SCHEMA
  )
  private[this] val generatorKeys = Set(
    PACKAGE_NAME, TEMPLATE, TEST_TEMPLATE, LINE_BREAK, CASE_CLASS_ONLY,
    ENCODING, AUTO_CONSTRUCT, DEFAULT_AUTO_SESSION, DATETIME_CLASS, RETURN_COLLECTION_TYPE,
    VIEW
  )
  private[this] val allKeys = jdbcKeys ++ generatorKeys

  private[this] def printWarningIfTypo(props: Properties): Unit = {
    import scala.collection.convert.decorateAsScala._
    props.keySet().asScala.map(_.toString).filterNot(allKeys).foreach { typoKey =>
      val correctKeys = allKeys.toList.sortBy(key => EditDistance.levenshtein(typoKey, key)).take(3).mkString(" or ")
      println(s"""Not a valid key "$typoKey". did you mean ${correctKeys}?""")
    }
  }

  private[this] def loadJDBCSettings(props: Properties): JDBCSettings = {
    printWarningIfTypo(props)
    JDBCSettings(
      driver = getString(props, JDBC_DRIVER).getOrElse(throw new IllegalStateException(s"Add $JDBC_DRIVER to project/scalikejdbc-mapper-generator.properties")),
      url = getString(props, JDBC_URL).getOrElse(throw new IllegalStateException(s"Add $JDBC_URL to project/scalikejdbc-mapper-generator.properties")),
      username = getString(props, JDBC_USER_NAME).getOrElse(""),
      password = getString(props, JDBC_PASSWORD).getOrElse(""),
      schema = getString(props, JDBC_SCHEMA).orNull[String]
    )
  }

  private[this] def loadGeneratorSettings(props: Properties): GeneratorSettings = {
    val defaultConfig = GeneratorConfig()
    GeneratorSettings(
      packageName = getString(props, PACKAGE_NAME).getOrElse(defaultConfig.packageName),
      template = getString(props, TEMPLATE).getOrElse(defaultConfig.template.name),
      testTemplate = getString(props, TEST_TEMPLATE).getOrElse(GeneratorTestTemplate.specs2unit.name),
      lineBreak = getString(props, LINE_BREAK).getOrElse(defaultConfig.lineBreak.name),
      caseClassOnly = getString(props, CASE_CLASS_ONLY).map(_.toBoolean).getOrElse(defaultConfig.caseClassOnly),
      encoding = getString(props, ENCODING).getOrElse(defaultConfig.encoding),
      autoConstruct = getString(props, AUTO_CONSTRUCT).map(_.toBoolean).getOrElse(defaultConfig.autoConstruct),
      defaultAutoSession = getString(props, DEFAULT_AUTO_SESSION).map(_.toBoolean).getOrElse(defaultConfig.defaultAutoSession),
      dateTimeClass = getString(props, DATETIME_CLASS).map {
        name => DateTimeClass.map.getOrElse(name, sys.error("does not support " + name))
      }.getOrElse(defaultConfig.dateTimeClass),
      defaultConfig.tableNameToClassName,
      defaultConfig.columnNameToFieldName,
      returnCollectionType = getString(props, RETURN_COLLECTION_TYPE).map { name =>
        ReturnCollectionType.map.getOrElse(name.toLowerCase(en), sys.error(s"does not support $name. support types are ${ReturnCollectionType.map.keys.mkString(", ")}"))
      }.getOrElse(defaultConfig.returnCollectionType),
      view = getString(props, VIEW).map(_.toBoolean).getOrElse(defaultConfig.view)
    )
  }

  private[this] def loadPropertiesFromFile(): Either[FileNotFoundException, Properties] = {
    val props = new java.util.Properties
    try {
      using(new java.io.FileInputStream("project/scalikejdbc-mapper-generator.properties")) {
        inputStream => props.load(inputStream)
      }
    } catch {
      case e: FileNotFoundException =>
    }
    if (props.isEmpty) {
      try {
        using(new java.io.FileInputStream("project/scalikejdbc.properties")) {
          inputStream => props.load(inputStream)
        }
        Right(props)
      } catch {
        case e: FileNotFoundException =>
          Left(e)
      }
    } else {
      Right(props)
    }
  }

  def generatorConfig(srcDir: File, testDir: File, generatorSettings: GeneratorSettings) =
    GeneratorConfig(
      srcDir = srcDir.getAbsolutePath,
      testDir = testDir.getAbsolutePath,
      packageName = generatorSettings.packageName,
      template = GeneratorTemplate(generatorSettings.template),
      testTemplate = GeneratorTestTemplate(generatorSettings.testTemplate),
      lineBreak = LineBreak(generatorSettings.lineBreak),
      caseClassOnly = generatorSettings.caseClassOnly,
      encoding = generatorSettings.encoding,
      autoConstruct = generatorSettings.autoConstruct,
      defaultAutoSession = generatorSettings.defaultAutoSession,
      dateTimeClass = generatorSettings.dateTimeClass,
      tableNameToClassName = generatorSettings.tableNameToClassName,
      columnNameToFieldName = generatorSettings.columnNameToFieldName,
      returnCollectionType = generatorSettings.returnCollectionType,
      view = generatorSettings.view
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
    val tableAndViews = if (generatorSettings.view) {
      model.allTables(jdbc.schema) ++ model.allViews(jdbc.schema)
    } else {
      model.allTables(jdbc.schema)
    }

    tableAndViews.map { table =>
      new CodeGenerator(table, className)(config)
    }
  }

  private final case class GenTaskParameter(table: String, clazz: Option[String])

  import complete.DefaultParsers._

  private def genTaskParser(keyName: String): complete.Parser[GenTaskParameter] = (
    Space ~> token(StringBasic, "tableName") ~ (Space ~> token(StringBasic, "(class-name)")).?
  ).map(GenTaskParameter.tupled).!!!("Usage: " + keyName + " [table-name (class-name)]")

  val scalikejdbcSettings = inConfig(Compile)(Seq(
    scalikejdbcCodeGeneratorSingle := { (table, clazz, jdbc, generatorSettings) =>
      val srcDir = (scalaSource in Compile).value
      val testDir = (scalaSource in Test).value
      generator(tableName = table, className = clazz, srcDir = srcDir, testDir = testDir, jdbc = jdbc, generatorSettings = generatorSettings)
    },
    scalikejdbcCodeGeneratorAll := { (jdbc, generatorSettings) =>
      val srcDir = (scalaSource in Compile).value
      val testDir = (scalaSource in Test).value
      allGenerators(srcDir = srcDir, testDir = testDir, jdbc = jdbc, generatorSettings = generatorSettings)
    },
    scalikejdbcGen := {
      val args = genTaskParser(scalikejdbcGen.key.label).parsed
      val gen = scalikejdbcCodeGeneratorSingle.value.apply(args.table, args.clazz, scalikejdbcJDBCSettings.value, scalikejdbcGeneratorSettings.value)
      gen.foreach { g =>
        g.writeModelIfNotExist()
        g.writeSpecIfNotExist(g.specAll())
      }
    },
    scalikejdbcGenForce := {
      val args = genTaskParser(scalikejdbcGenForce.key.label).parsed
      val gen = scalikejdbcCodeGeneratorSingle.value.apply(args.table, args.clazz, scalikejdbcJDBCSettings.value, scalikejdbcGeneratorSettings.value)
      gen.foreach { g =>
        g.writeModel()
        g.writeSpec(g.specAll())
      }
    },
    scalikejdbcGenAll := {
      scalikejdbcCodeGeneratorAll.value.apply(scalikejdbcJDBCSettings.value, scalikejdbcGeneratorSettings.value).foreach { g =>
        g.writeModelIfNotExist()
        g.writeSpecIfNotExist(g.specAll())
      }
    },
    scalikejdbcGenAllForce := {
      scalikejdbcCodeGeneratorAll.value.apply(scalikejdbcJDBCSettings.value, scalikejdbcGeneratorSettings.value).foreach { g =>
        g.writeModel()
        g.writeSpec(g.specAll())
      }
    },
    scalikejdbcGenEcho := {
      val args = genTaskParser(scalikejdbcGenEcho.key.label).parsed
      val gen = scalikejdbcCodeGeneratorSingle.value.apply(args.table, args.clazz, scalikejdbcJDBCSettings.value, scalikejdbcGeneratorSettings.value)
      gen.foreach(g => println(g.modelAll()))
      gen.foreach(g => g.specAll().foreach(spec => println(spec)))
    },
    scalikejdbcJDBCSettings := loadPropertiesFromFile().fold(throw _, loadJDBCSettings),
    scalikejdbcGeneratorSettings := loadPropertiesFromFile().fold(throw _, loadGeneratorSettings)
  ))

  def using[R <: { def close() }, A](resource: R)(f: R => A): A = ultimately {
    ignoring(classOf[Throwable]) apply resource.close()
  } apply f(resource)

}

