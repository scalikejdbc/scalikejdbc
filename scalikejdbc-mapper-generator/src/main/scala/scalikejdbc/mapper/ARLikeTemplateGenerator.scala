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

import scalikejdbc._

/**
 * Active Record like template generator
 */
case class ARLikeTemplateGenerator(table: Table, specifiedClassName: Option[String] = None)(implicit config: GeneratorConfig = GeneratorConfig()) {

  import java.sql.{ Types => JavaSqlTypes }
  import java.io.{ OutputStreamWriter, FileOutputStream, File }

  private val packageName = config.packageName
  private val className = specifiedClassName.getOrElse(toClassName(table))
  private val comma = ","
  private val eol = config.lineBreak.value

  object TypeName {
    val Any = "Any"
    val AnyArray = "Array[Any]"
    val ByteArray = "Array[Byte]"
    val Long = "Long"
    val Boolean = "Boolean"
    val DateTime = "DateTime"
    val LocalDate = "LocalDate"
    val LocalTime = "LocalTime"
    val String = "String"
    val Byte = "Byte"
    val Int = "Int"
    val Short = "Short"
    val Float = "Float"
    val Double = "Double"
    val Blob = "Blob"
    val Clob = "Clob"
    val Ref = "Ref"
    val Struct = "Struct"
    val BigDecimal = "BigDecimal" // scala.math.BigDecimal
  }

  case class IndentGenerator(i: Int) {
    def indent: String = " " * i * 2
  }

  implicit def convertIntToIndentGenerator(i: Int) = IndentGenerator(i)

  case class ColumnInScala(underlying: Column) {

    lazy val nameInScala: String = {
      val camelCase: String = toCamelCase(underlying.name)
      camelCase.head.toLower + camelCase.tail
    }

    lazy val rawTypeInScala: String = underlying.dataType match {
      case JavaSqlTypes.ARRAY => TypeName.AnyArray
      case JavaSqlTypes.BIGINT => TypeName.Long
      case JavaSqlTypes.BINARY => TypeName.ByteArray
      case JavaSqlTypes.BIT => TypeName.Boolean
      case JavaSqlTypes.BLOB => TypeName.Blob
      case JavaSqlTypes.BOOLEAN => TypeName.Boolean
      case JavaSqlTypes.CHAR => TypeName.String
      case JavaSqlTypes.CLOB => TypeName.Clob
      case JavaSqlTypes.DATALINK => TypeName.Any
      case JavaSqlTypes.DATE => TypeName.LocalDate
      case JavaSqlTypes.DECIMAL => TypeName.BigDecimal
      case JavaSqlTypes.DISTINCT => TypeName.Any
      case JavaSqlTypes.DOUBLE => TypeName.Double
      case JavaSqlTypes.FLOAT => TypeName.Float
      case JavaSqlTypes.INTEGER => TypeName.Int
      case JavaSqlTypes.JAVA_OBJECT => TypeName.Any
      case JavaSqlTypes.LONGVARBINARY => TypeName.ByteArray
      case JavaSqlTypes.LONGVARCHAR => TypeName.String
      case JavaSqlTypes.NULL => TypeName.Any
      case JavaSqlTypes.NUMERIC => TypeName.BigDecimal
      case JavaSqlTypes.OTHER => TypeName.Any
      case JavaSqlTypes.REAL => TypeName.Float
      case JavaSqlTypes.REF => TypeName.Ref
      case JavaSqlTypes.SMALLINT => TypeName.Short
      case JavaSqlTypes.STRUCT => TypeName.Struct
      case JavaSqlTypes.TIME => TypeName.LocalTime
      case JavaSqlTypes.TIMESTAMP => TypeName.DateTime
      case JavaSqlTypes.TINYINT => TypeName.Byte
      case JavaSqlTypes.VARBINARY => TypeName.ByteArray
      case JavaSqlTypes.VARCHAR => TypeName.String
      case _ => TypeName.Any
    }

    lazy val typeInScala: String = {
      if (underlying.isNotNull) rawTypeInScala
      else "Option[" + rawTypeInScala + "]"
    }

    lazy val extractorName: String = underlying.dataType match {
      case JavaSqlTypes.ARRAY => "array"
      case JavaSqlTypes.BIGINT => "long"
      case JavaSqlTypes.BINARY => "bytes"
      case JavaSqlTypes.BIT => "boolean"
      case JavaSqlTypes.BLOB => "blob"
      case JavaSqlTypes.BOOLEAN => "boolean"
      case JavaSqlTypes.CHAR => "string"
      case JavaSqlTypes.CLOB => "clob"
      case JavaSqlTypes.DATALINK => "any"
      case JavaSqlTypes.DATE => "date"
      case JavaSqlTypes.DECIMAL => "bigDecimal"
      case JavaSqlTypes.DISTINCT => "any"
      case JavaSqlTypes.DOUBLE => "double"
      case JavaSqlTypes.FLOAT => "float"
      case JavaSqlTypes.INTEGER => "int"
      case JavaSqlTypes.JAVA_OBJECT => "any"
      case JavaSqlTypes.LONGVARBINARY => "bytes"
      case JavaSqlTypes.LONGVARCHAR => "string"
      case JavaSqlTypes.NULL => "any"
      case JavaSqlTypes.NUMERIC => "bigDecimal"
      case JavaSqlTypes.OTHER => "any"
      case JavaSqlTypes.REAL => "float"
      case JavaSqlTypes.REF => "ref"
      case JavaSqlTypes.SMALLINT => "short"
      case JavaSqlTypes.STRUCT => "any"
      case JavaSqlTypes.TIME => "time"
      case JavaSqlTypes.TIMESTAMP => "timestamp"
      case JavaSqlTypes.TINYINT => "byte"
      case JavaSqlTypes.VARBINARY => "bytes"
      case JavaSqlTypes.VARCHAR => "string"
      case _ => "any"
    }

    lazy val dummyValue: String = underlying.dataType match {
      case JavaSqlTypes.ARRAY => "null"
      case JavaSqlTypes.BIGINT => "1"
      case JavaSqlTypes.BINARY => "1"
      case JavaSqlTypes.BIT => "false"
      case JavaSqlTypes.BLOB => "null"
      case JavaSqlTypes.BOOLEAN => "boolean"
      case JavaSqlTypes.CHAR => "'abc'"
      case JavaSqlTypes.CLOB => "null"
      case JavaSqlTypes.DATALINK => "null"
      case JavaSqlTypes.DATE => "'1958-09-06'"
      case JavaSqlTypes.DECIMAL => "1"
      case JavaSqlTypes.DISTINCT => "null"
      case JavaSqlTypes.DOUBLE => "0.1"
      case JavaSqlTypes.FLOAT => "0.1"
      case JavaSqlTypes.INTEGER => "1"
      case JavaSqlTypes.JAVA_OBJECT => "null"
      case JavaSqlTypes.LONGVARBINARY => "null"
      case JavaSqlTypes.LONGVARCHAR => "'abc'"
      case JavaSqlTypes.NULL => "null"
      case JavaSqlTypes.NUMERIC => "1"
      case JavaSqlTypes.OTHER => "null"
      case JavaSqlTypes.REAL => "null"
      case JavaSqlTypes.REF => "null"
      case JavaSqlTypes.SMALLINT => "1"
      case JavaSqlTypes.STRUCT => "null"
      case JavaSqlTypes.TIME => "'12:00:00'"
      case JavaSqlTypes.TIMESTAMP => "'1958-09-06 12:00:00'"
      case JavaSqlTypes.TINYINT => "1"
      case JavaSqlTypes.VARBINARY => "null"
      case JavaSqlTypes.VARCHAR => "'abc'"
      case _ => "null"
    }

  }

  implicit def convertColumnToColumnInScala(column: Column): ColumnInScala = ColumnInScala(column)

  /**
   * Write the source code to file.
   */
  def writeFileIfNotExist(): Unit = {
    val file = new File(config.srcDir + "/" + packageName.replaceAll("\\.", "/") + "/" + className + ".scala")
    if (file.exists) {
      println("\"" + packageName + "." + className + "\"" + " already exists.")
    } else {
      mkdirRecursively(file.getParentFile)
      using(new FileOutputStream(file)) {
        fos =>
          using(new OutputStreamWriter(fos)) {
            writer =>
              writer.write(generateAll())
              println("\"" + packageName + "." + className + "\"" + " created.")
          }
      }
    }
  }

  /**
   * Create directory to put the source code file if it does not exist yet.
   */
  def mkdirRecursively(file: File): Unit = {
    if (!file.getParentFile.exists) mkdirRecursively(file.getParentFile)
    if (!file.exists) file.mkdir()
  }

  /**
   * Class part.
   *
   * {{{
   * case class Member(id: Long, name: String, description: Option[String])) {
   *   def save(): Member = Member.save(this)
   *   def destroy(): Unit = Member.delete(this)
   * }
   * }}}
   */
  def classPart: String = {
    if (table.allColumns.size <= 22) {
      "case class " + className + "(" + eol +
        table.allColumns.map {
          c => 1.indent + c.nameInScala + ": " + c.typeInScala + (if (c.isNotNull) "" else " = None")
        }.mkString(", " + eol) + ") { " + eol +
        eol +
        1.indent + "def save()(implicit session: DBSession = " + className + ".autoSession): " + className + " = " + className + ".save(this)(session)" + eol +
        eol +
        1.indent + "def destroy()(implicit session: DBSession = " + className + ".autoSession): Unit = " + className + ".delete(this)(session)" + eol +
        eol +
        "}"
    } else {
      "class " + className + " (" + eol +
        table.allColumns.map {
          c =>
            1.indent + "val " + c.nameInScala + ": " + c.typeInScala + (if (c.isNotNull) "" else " = None")
        }.mkString(comma + eol) + ") { " + eol +
        eol +
        1.indent + "def copy(" + eol +
        table.allColumns.map {
          c => 2.indent + c.nameInScala + ": " + c.typeInScala + " = this." + c.nameInScala
        }.mkString(comma + eol) + "): " + className + " = {" + eol +
        2.indent + "new " + className + "(" + eol +
        table.allColumns.map {
          c => 3.indent + c.nameInScala + " = " + c.nameInScala
        }.mkString(comma + eol) + eol +
        2.indent + ")" + eol +
        1.indent + "}" + eol +
        eol +
        1.indent + "def save(): " + className + " = " + className + ".save(this)" + eol +
        eol +
        1.indent + "def destroy(): Unit = " + className + ".delete(this)" + eol +
        eol +
        "}"
    }
  }

  /**
   * {{{
   * object Member {
   *   // ... as follows
   * }
   * }}}
   */
  def objectPart: String = {

    val allColumns = table.allColumns
    val pkColumns = if (table.primaryKeyColumns.size == 0) allColumns else table.primaryKeyColumns

    /**
     * {{{
     * val tableName = "MEMBER"
     * }}}
     */
    val tableName = 1.indent + "val tableName = \"" + table.name + "\"" + eol

    /**
     * {{{
     * object columnNames {
     *   val id = "ID"
     *   val name = "NAME"
     *   val birthday = "BIRTHDAY"
     *   val all = Seq(id, name, birthday)
     * }
     * }}}
     */
    val columnNames = {
      1.indent + "object columnNames {" + eol +
        allColumns.map {
          c => 2.indent + "val " + c.nameInScala + " = \"" + c.name + "\""
        }.mkString(eol) + eol +
        2.indent + "val all = Seq(" + allColumns.map {
          c => c.nameInScala
        }.mkString(", ") + ")" + eol +
        1.indent + "}" + eol
    }

    /**
     * {{{
     * val * = {
     *   import columnNames._
     *   (rs: WrappedResultSet) => Member(
     *     rs.long(id),
     *     rs.string(name),
     *     Option(rs.date(birthday)).map(_.toLocalDate)
     *   )
     * }
     * }}}
     */

    val _mapper = {
      2.indent + "(rs: WrappedResultSet) => " + (if (allColumns.size > 22) "new " else "") + className + "(" + eol +
        allColumns.map {
          c =>
            if (c.isNotNull) 3.indent + c.nameInScala + " = rs." + c.extractorName + "(" + c.nameInScala + ")" + cast(c, false)
            else 3.indent + c.nameInScala + " = " + toOption(c) + "(rs." + c.extractorName + "(" + c.nameInScala + ")" + cast(c, true) + ")"
        }.mkString(comma + eol) + ")" + eol
    }

    val mapper = {
      1.indent + "val * = {" + eol +
        2.indent + "import columnNames._" + eol +
        _mapper +
        1.indent + "}" + eol
    }

    /**
     * {{{
     * object joinedColumnNames {
     *   val delimiter = "__ON__"
     *   def as(name: String) = name + delimiter + tableName
     *   val id = as(columnNames.id)
     *   val name = as(columnNames.name)
     *   val birthday = as(columnNames.birthday)
     *   val all = Seq(id, name, birthday)
     *   val inSQL = columnNames.all.map(name => tableName + "." + name + " AS " + as(name)).mkString(", ")
     * }
     * }}}
     */
    val joinedColumnNames = {
      1.indent + "object joinedColumnNames {" + eol +
        2.indent + "val delimiter = \"__ON__\"" + eol +
        2.indent + "def as(name: String) = name + delimiter + tableName" + eol +
        allColumns.map {
          c => 2.indent + "val " + c.nameInScala + " = as(columnNames." + c.nameInScala + ")"
        }.mkString(eol) + eol +
        2.indent + "val all = Seq(" + allColumns.map {
          c => c.nameInScala
        }.mkString(", ") + ")" + eol +
        2.indent + "val inSQL = columnNames.all.map(name => tableName + \".\" + name + \" AS \" + as(name)).mkString(\", \")" + eol +
        1.indent + "}" + eol
    }

    /**
     * {{{
     * val joined = {
     *   import joinedColumnNames._
     *   (rs: WrappedResultSet) => Member(
     *     rs.long(id),
     *     rs.string(name),
     *     Option(rs.date(birthday)).map(_.toLocalDate)
     *   )
     * }
     * }}}
     */
    val joinedMapper = {
      1.indent + "val joined = {" + eol +
        2.indent + "import joinedColumnNames._" + eol +
        _mapper +
        1.indent + "}" + eol
    }

    /**
     * {{{
     * val autoSession = AutoSession
     * }}}
     */
    val autoSession = 1.indent + "val autoSession = AutoSession" + eol

    /**
     * {{{
     * def create(
     *   name: String,
     *   birthday: Option[LocalDate])
     *   (implicit session: DBSession = autoSession): Member = {
     *   val generatedKey = SQL("""
     *     INSERT INTO MEMBER (
     *       NAME,
     *       BIRTHDAY
     *     ) VALUES (
     *       /*'name*/'abc',
     *       /*'birthday*/'1958-09-06'
     *     )
     *   """)
     *     .bindByName(
     *       'name -> name,
     *       'birthday -> birthday
     *     ).updateAndReturnGeneratedKey.apply()
     *   Member(
     *     id = generatedKey,
     *     name = name,
     *     birthday = birthday
     *   )
     * }
     * }}}
     */
    val createMethod = {
      val createColumns: List[Column] = allColumns.filterNot {
        c => table.autoIncrementColumns.find(aic => aic.name == c.name).isDefined
      }

      val placeHolderPart: String = config.template match {
        case GeneratorTemplate.placeHolderSQL =>
          (1 to createColumns.size).map(c => 4.indent + "?").mkString(comma + eol)
        case GeneratorTemplate.anormSQL =>
          createColumns.map(c => 4.indent + "{" + c.nameInScala + "}").mkString(comma + eol)
        case GeneratorTemplate.execautableSQL =>
          createColumns.map(c => 4.indent + "/*'" + c.nameInScala + "*/" + c.dummyValue).mkString(comma + eol)
      }

      val bindingPart: String = config.template match {
        case GeneratorTemplate.placeHolderSQL =>
          3.indent + ".bind(" + eol +
            createColumns.map(c => 4.indent + c.nameInScala).mkString(comma + eol)
        case GeneratorTemplate.anormSQL | GeneratorTemplate.execautableSQL =>
          3.indent + ".bindByName(" + eol +
            createColumns.map {
              c => 4.indent + "'" + c.nameInScala + " -> " + c.nameInScala
            }.mkString(comma + eol)
      }

      1.indent + "def create(" + eol +
        createColumns.map {
          c => 2.indent + c.nameInScala + ": " + c.typeInScala + (if (c.isNotNull) "" else " = None")
        }.mkString(comma + eol) + ")(implicit session: DBSession = autoSession): " + className + " = {" + eol +
        2.indent + table.autoIncrementColumns.headOption.map(_ => "val generatedKey = ").getOrElse("") +
        "SQL(\"\"\"" + eol +
        3.indent + "INSERT INTO " + table.name + " (" + eol +
        createColumns.map(c => 4.indent + c.name).mkString(comma + eol) + eol +
        3.indent + ") VALUES (" + eol +
        placeHolderPart + eol +
        3.indent + ")" + eol +
        3.indent + "\"\"\")" + eol +
        bindingPart + eol +
        3.indent +
        table.autoIncrementColumns.headOption.map(_ => ").updateAndReturnGeneratedKey.apply()").getOrElse(").update.apply()") +
        eol +
        2.indent + (if (allColumns.size > 22) "new " else "") + className + "(" + eol +
        table.autoIncrementColumns.headOption.map {
          c =>
            3.indent + c.nameInScala +
              (c.typeInScala match {
                case TypeName.Byte => " = generatedKey.toByte, "
                case TypeName.Int => " = generatedKey.toInt, "
                case TypeName.Short => " = generatedKey.toShort, "
                case TypeName.Float => " = generatedKey.toFloat, "
                case TypeName.Double => " = generatedKey.toDouble, "
                case TypeName.String => " = generatedKey.toString, "
                case TypeName.BigDecimal => " = BigDecimal.valueOf(generatedKey), "
                case _ => " = generatedKey, "
              }) + eol
        }.getOrElse("") +
        createColumns.map {
          c => 3.indent + c.nameInScala + " = " + c.nameInScala
        }.mkString(comma + eol) + ")" + eol +
        1.indent + "}" + eol
    }

    /**
     * {{{
     * def save(m: Member)(implicit session: DBSession = autoSession): Member = {
     *   SQL("""
     *     UPDATE
     *       MEMBER
     *     SET
     *       ID = /*'id*/123,
     *       NAME = /*'name*/'abc',
     *       BIRTHDAY = /*'birthday*/'1958-09-06'
     *     WHERE
     *       ID = /*'id*/123
     *   """)
     *     .bindByName(
     *       'id -> m.id,
     *       'name -> m.name,
     *       'birthday -> m.birthday
     *     ).update.apply()
     *   m
     * }
     * }}}
     */
    val saveMethod = {

      val placeHolderPart: String = config.template match {
        case GeneratorTemplate.placeHolderSQL =>
          allColumns.map(c => 4.indent + c.name + " = ?").mkString(comma + eol)
        case GeneratorTemplate.anormSQL =>
          allColumns.map(c => 4.indent + c.name + " = {" + c.nameInScala + "}").mkString(comma + eol)
        case GeneratorTemplate.execautableSQL =>
          allColumns.map(c => 4.indent + c.name + " = /*'" + c.nameInScala + "*/" + c.dummyValue).mkString(comma + eol)
      }

      val wherePart = config.template match {
        case GeneratorTemplate.placeHolderSQL =>
          4.indent + pkColumns.map(pk => pk.name + " = ?").mkString(" AND ")
        case GeneratorTemplate.anormSQL =>
          4.indent + pkColumns.map(pk => pk.name + " = {" + pk.nameInScala + "}").mkString(" AND ")
        case GeneratorTemplate.execautableSQL =>
          4.indent + pkColumns.map(pk => pk.name + " = /*'" + pk.nameInScala + "*/" + pk.dummyValue).mkString(" AND ")
      }

      val bindingPart = config.template match {
        case GeneratorTemplate.placeHolderSQL =>
          3.indent + ".bind(" + eol +
            allColumns.map(c => 4.indent + "m." + c.nameInScala).mkString(comma + eol) + ", " + eol +
            pkColumns.map(pk => 4.indent + "m." + pk.nameInScala).mkString(comma + eol)
        case GeneratorTemplate.anormSQL | GeneratorTemplate.execautableSQL =>
          3.indent + ".bindByName(" + eol +
            allColumns.map(c => 4.indent + "'" + c.nameInScala + " -> m." + c.nameInScala).mkString(comma + eol)
      }

      1.indent + "def save(m: " + className + ")(implicit session: DBSession = autoSession): " + className + " = {" + eol +
        2.indent + "SQL(\"\"\"" + eol +
        3.indent + "UPDATE " + eol +
        4.indent + table.name + eol +
        3.indent + "SET " + eol +
        placeHolderPart + eol +
        3.indent + "WHERE " + eol +
        wherePart + eol +
        3.indent + "\"\"\")" + eol +
        bindingPart + eol +
        3.indent + ").update.apply()" + eol +
        2.indent + "m" + eol +
        1.indent + "}" + eol
    }

    /**
     * {{{
     * def delete(m: Member)(implicit session: DBSession = autoSession): Unit = {
     *   SQL("""DELETE FROM MEMBER WHERE ID = /*'id*/123""")
     *     .bindByName('id -> m.id)
     *     .update.apply()
     * }
     * }}}
     */
    val deleteMethod = {

      val wherePart: String = config.template match {
        case GeneratorTemplate.placeHolderSQL =>
          pkColumns.map(pk => pk.name + " = ?").mkString(" AND ")
        case GeneratorTemplate.anormSQL =>
          pkColumns.map(pk => pk.name + " = {" + pk.nameInScala + "}").mkString(" AND ")
        case GeneratorTemplate.execautableSQL =>
          pkColumns.map(pk => pk.name + " = /*'" + pk.nameInScala + "*/" + pk.dummyValue).mkString(" AND ")
      }

      val bindingPart: String = config.template match {
        case GeneratorTemplate.placeHolderSQL =>
          ".bind(" + pkColumns.map(pk => "m." + pk.nameInScala).mkString(", ")
        case GeneratorTemplate.anormSQL | GeneratorTemplate.execautableSQL =>
          ".bindByName(" + pkColumns.map(pk => "'" + pk.nameInScala + " -> m." + pk.nameInScala).mkString(", ")
      }

      1.indent + "def delete(m: " + className + ")(implicit session: DBSession = autoSession): Unit = {" + eol +
        2.indent + "SQL(\"\"\"DELETE FROM " + table.name + " WHERE " + wherePart + "\"\"\")" + eol +
        3.indent + bindingPart + ").update.apply()" + eol +
        1.indent + "}" + eol
    }

    /**
     * {{{
     * def find(id: Long): Option[Member] = {
     *   DB readOnly { implicit session =>
     *     SQL("""SELECT * FROM MEMBER WHERE ID = /*'id*/123""")
     *       .bindByName('id -> id).map(*).single.apply()
     *   }
     * }
     * }}}
     */
    val findMethod =
      1.indent + "def find(" + pkColumns.map(pk => pk.nameInScala + ": " + pk.typeInScala).mkString(", ") +
        ")(implicit session: DBSession = autoSession): Option[" + className + "] = {" + eol +
        2.indent + "SQL(\"\"\"SELECT * FROM " + table.name + " WHERE " + (config.template match {
          case GeneratorTemplate.placeHolderSQL =>
            pkColumns.map(pk => pk.name + " = ?").mkString(" AND ")
          case GeneratorTemplate.anormSQL =>
            pkColumns.map(pk => pk.name + " = {" + pk.nameInScala + "}").mkString(" AND ")
          case GeneratorTemplate.execautableSQL =>
            pkColumns.map(pk => pk.name + " = /*'" + pk.nameInScala + "*/" + pk.dummyValue).mkString(" AND ")
        }) + "\"\"\")" + eol +
        3.indent + (config.template match {
          case GeneratorTemplate.placeHolderSQL =>
            ".bind(" + pkColumns.map(pk => pk.nameInScala).mkString(", ")
          case GeneratorTemplate.anormSQL | GeneratorTemplate.execautableSQL =>
            ".bindByName(" + pkColumns.map(pk => "'" + pk.nameInScala + " -> " + pk.nameInScala).mkString(", ")
        }) + ").map(*).single.apply()" + eol +
        1.indent + "}" + eol

    /**
     * {{{
     * def countAll(): Long = {
     *   DB readOnly { implicit session =>
     *     SQL("""SELECT COUNT(1) FROM MEMBER""")
     *       .map(rs => rs.long(1)).single.apply().get
     *   }
     * }
     * }}}
     */
    val countAllMethod =
      1.indent + "def countAll()(implicit session: DBSession = autoSession): Long = {" + eol +
        2.indent + "SQL(\"\"\"SELECT COUNT(1) FROM " + table.name + "\"\"\").map(rs => rs.long(1)).single.apply().get" + eol +
        1.indent + "}" + eol

    /**
     * {{{
     * def findAll(): List[Member] = {
     *   DB readOnly { implicit session =>
     *     SQL("""SELECT * FROM MEMBER""").map(*).list.apply()
     *   }
     * }
     * }}}
     */
    val findAllMethod =
      1.indent + "def findAll()(implicit session: DBSession = autoSession): List[" + className + "] = {" + eol +
        2.indent + "SQL(\"\"\"SELECT * FROM " + table.name + "\"\"\").map(*).list.apply()" + eol +
        1.indent + "}" + eol

    /**
     * {{{
     * def findAllBy(where: String, params:(Symbol, Any)*): List[Member] = {
     *   DB readOnly { implicit session =>
     *     SQL("""SELECT * FROM MEMBER """ + where)
     *       .bindByName(params:_*).map(*).list.apply()
     *   }
     * }
     * }}}
     */
    val findAllByMethod =
      1.indent + "def findAllBy(where: String, " + (config.template match {
        case GeneratorTemplate.placeHolderSQL => "params: Any*"
        case GeneratorTemplate.anormSQL | GeneratorTemplate.execautableSQL => "params: (Symbol, Any)*"
      }) + ")(implicit session: DBSession = autoSession): List[" + className + "] = {" + eol +
        2.indent + "SQL(\"\"\"SELECT * FROM " + table.name + " WHERE \"\"\" + where)" + eol +
        3.indent + (config.template match {
          case GeneratorTemplate.placeHolderSQL => ".bind"
          case GeneratorTemplate.anormSQL | GeneratorTemplate.execautableSQL => ".bindByName"
        }) + "(params:_*).map(*).list.apply()" + eol +
        1.indent + "}" + eol

    /**
     * {{{
     * def countBy(where: String, params:(Symbol, Any)*): Long = {
     *   DB readOnly { implicit session =>
     *     SQL("""SELECT COUNT(1) FROM MEMBER """ + where)
     *       .bindByName(params:_*).map(*).single.apply().get
     *   }
     * }
     * }}}
     */
    val countByMethod =
      1.indent + "def countBy(where: String, " + (config.template match {
        case GeneratorTemplate.placeHolderSQL => "params: Any*"
        case GeneratorTemplate.anormSQL | GeneratorTemplate.execautableSQL => "params: (Symbol, Any)*"
      }) + ")(implicit session: DBSession = autoSession): Long = {" + eol +
        2.indent + "SQL(\"\"\"SELECT count(1) FROM " + table.name + " WHERE \"\"\" + where)" + eol +
        3.indent + (config.template match {
          case GeneratorTemplate.placeHolderSQL => ".bind"
          case GeneratorTemplate.anormSQL | GeneratorTemplate.execautableSQL => ".bindByName"
        }) + "(params:_*).map(rs => rs.long(1)).single.apply().get" + eol +
        1.indent + "}" + eol

    "object " + className + " {" + eol +
      eol +
      tableName +
      eol +
      columnNames +
      eol +
      mapper +
      eol +
      joinedColumnNames +
      eol +
      joinedMapper +
      eol +
      autoSession +
      eol +
      findMethod +
      eol +
      findAllMethod +
      eol +
      countAllMethod +
      eol +
      findAllByMethod +
      eol +
      countByMethod +
      eol +
      createMethod +
      eol +
      saveMethod +
      eol +
      deleteMethod +
      eol +
      "}"
  }

  def generateAll(): String = {
    val jodaTimeImport = table.allColumns.flatMap {
      c =>
        c.rawTypeInScala match {
          case TypeName.DateTime => Some("DateTime")
          case TypeName.LocalDate => Some("LocalDate")
          case TypeName.LocalTime => Some("LocalTime")
          case _ => None
        }
    } match {
      case classes if classes.size > 0 => "import org.joda.time.{" + classes.distinct.mkString(", ") + "}" + eol
      case _ => ""
    }
    val javaSqlImport = table.allColumns.flatMap {
      c =>
        c.rawTypeInScala match {
          case TypeName.Blob => Some("Blob")
          case TypeName.Clob => Some("Clob")
          case TypeName.Ref => Some("Ref")
          case TypeName.Struct => Some("Struct")
          case _ => None
        }
    } match {
      case classes if classes.size > 0 => "import java.sql.{" + classes.distinct.mkString(", ") + "}" + eol
      case _ => ""
    }
    "package " + config.packageName + eol +
      eol +
      "import scalikejdbc._" + eol +
      jodaTimeImport +
      javaSqlImport +
      eol +
      classPart + eol +
      eol +
      objectPart + eol
  }

  private def toOption(column: Column): String = column.dataType match {
    case JavaSqlTypes.BIGINT |
      JavaSqlTypes.BIT |
      JavaSqlTypes.BOOLEAN |
      JavaSqlTypes.DOUBLE |
      JavaSqlTypes.FLOAT |
      JavaSqlTypes.REAL |
      JavaSqlTypes.INTEGER |
      JavaSqlTypes.SMALLINT |
      JavaSqlTypes.TINYINT => "opt[" + column.rawTypeInScala + "]"
    case _ => "Option"
  }

  private def cast(column: Column, optional: Boolean): String = column.dataType match {
    case JavaSqlTypes.DATE if optional => ").map(_.toLocalDate"
    case JavaSqlTypes.DATE => ".toLocalDate"
    case JavaSqlTypes.DECIMAL => ".toScalaBigDecimal"
    case JavaSqlTypes.TIME if optional => ").map(_.toLocalTime"
    case JavaSqlTypes.TIME => ".toLocalTime"
    case JavaSqlTypes.TIMESTAMP if optional => ").map(_.toDateTime"
    case JavaSqlTypes.TIMESTAMP => ".toDateTime"
    case _ => ""
  }

  private def toClassName(table: Table): String = toCamelCase(table.name)

  private def toCamelCase(s: String): String = s.split("_").toList.foldLeft("") {
    (camelCaseString, part) =>
      camelCaseString + toProperCase(part)
  }

  private def toProperCase(s: String): String = {
    s.substring(0, 1).toUpperCase + s.substring(1).toLowerCase
  }

}

