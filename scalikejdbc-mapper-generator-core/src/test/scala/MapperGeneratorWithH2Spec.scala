import scalikejdbc._
import mapper._
import mapper.CodeGenerator
import mapper.GeneratorConfig
import mapper.Model
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MapperGeneratorWithH2Spec extends AnyFlatSpec with Matchers {

  Class.forName("org.h2.Driver")
  val url = "jdbc:h2:mem:mapper-generator-h2"
  val username = "sa"
  val password = ""
  ConnectionPool.singleton(url, username, password)

  val srcDir = "scalikejdbc-mapper-generator-core/target/generated_src"

  it should "work fine with member_group" in {
    DB autoCommit { implicit session =>
      SQL("""
        create table member_group (
          id int generated always as identity,
          name varchar(30) not null,
          _underscore varchar(30),
          primary key(id)
        )
      """).execute.apply()
    }
    Model(url, username, password).table(null, "MEMBER_GROUP").map { table =>
      {
        val generator = new CodeGenerator(table)(using
          GeneratorConfig(
            srcDir = srcDir,
            template = GeneratorTemplate.interpolation,
            packageName = "com.example.interpolation"
          )
        )
        generator.modelAll()
        generator.writeModel()
      }

      {
        val generator = new CodeGenerator(table)(using
          GeneratorConfig(
            srcDir = srcDir,
            template = GeneratorTemplate.queryDsl,
            packageName = "com.example.querydsl"
          )
        )
        generator.modelAll()
        generator.writeModel()
      }

    } getOrElse {
      fail("The table is not found.")
    }
    Thread.sleep(500)
  }

  it should "work fine with member" in {
    DB autoCommit { implicit session =>
      SQL("""
        create table member (
          id int
          generated always as identity,
          name varchar(30) not null,
          member_group_id int,
          description varchar(1000),
          birthday date,
          created_at timestamp not null,
          primary key(id)
        )
      """).execute.apply()
    }

    Model(url, username, password).table(null, "MEMBER").map { table =>
      val generator1 = new CodeGenerator(table)(using
        GeneratorConfig(
          srcDir = srcDir,
          template = GeneratorTemplate.queryDsl,
          testTemplate = GeneratorTestTemplate("specs2unit"),
          packageName = "com.example"
        )
      )
      generator1.specAll()
      generator1.writeModel()
      val generator2 = new CodeGenerator(
        table
      )(using
        GeneratorConfig(
          srcDir = srcDir,
          template = GeneratorTemplate.queryDsl,
          testTemplate = GeneratorTestTemplate("specs2acceptance"),
          packageName = "com.example.placeholder"
        )
      )
      generator2.specAll()
      generator2.writeModel()

      val generator3 = new CodeGenerator(
        table
      )(using
        GeneratorConfig(
          srcDir = srcDir,
          template = GeneratorTemplate.queryDsl,
          testTemplate = GeneratorTestTemplate("ScalaTestFlatSpec"),
          packageName = "com.example.anorm"
        )
      )
      generator3.specAll()
      generator3.writeModel()

      val generator4 = new CodeGenerator(
        table.copy(schema = Some("public"))
      )(using
        GeneratorConfig(
          srcDir = srcDir,
          template = GeneratorTemplate.queryDsl,
          testTemplate = GeneratorTestTemplate("ScalaTestFlatSpec"),
          packageName = "com.example.schema"
        )
      )
      generator4.specAll()
      generator4.writeModel()

      val generator5 = new CodeGenerator(
        table.copy(schema = Some(""))
      )(using
        GeneratorConfig(
          srcDir = srcDir,
          template = GeneratorTemplate.queryDsl,
          testTemplate = GeneratorTestTemplate("ScalaTestFlatSpec"),
          packageName = "com.example.schema2"
        )
      )
      generator5.specAll()
      generator5.writeModel()

    } getOrElse {
      fail("The table is not found.")
    }
    Thread.sleep(500)
  }

  it should "work fine with large table" in {

    DB autoCommit { implicit session =>
      SQL("""
        create table un_normalized (
          id bigint generated always as identity,
          v_01 TINYINT not null,
          v_02 SMALLINT not null,
          v_03 INTEGER not null,
          v_04 BIGINT not null,
          v_05 NUMERIC not null,
          v_06 DECIMAL(10,2) not null,
          v_07 DOUBLE not null,
          v_08 BOOLEAN,
          v_09 CHAR(10),
          v_10 VARCHAR(20) not null,
          v_11 TINYINT,
          v_12 SMALLINT,
          v_13 INTEGER,
          v_14 BIGINT,
          v_15 NUMERIC,
          v_16 BIT(10),
          v_17 DATE not null,
          v_18 TIME not null,
          v_19 TIME(6) not null,
          v_20 TIMESTAMP not null,
          v_21 VARCHAR(2),
          v_22 BOOLEAN not null,
          v_23 REAL not null,
          v_24 FLOAT not null,
          created_at timestamp not null,
          primary key(id)
        )
      """).execute.apply()
    }

    Model(url, username, password).table(null, "UN_NORMALIZED").map { table =>
      val generator = new CodeGenerator(table)(using
        GeneratorConfig(srcDir = srcDir, packageName = "com.example")
      )
      generator.writeModel()
    } getOrElse {
      fail("The table is not found.")
    }

    Thread.sleep(500)
  }

  it should "work fine with without_pk" in {
    DB autoCommit { implicit session =>
      SQL("""
        create table without_pk (
          aaa varchar(30) not null,
          bbb int,
          created_at timestamp not null
        )
      """).execute.apply()
    }

    Model(url, username, password).table(null, "WITHOUT_PK").map { table =>
      val generator = new CodeGenerator(table)(using
        GeneratorConfig(srcDir = srcDir, packageName = "com.example")
      )
      generator.writeModel()
    } getOrElse {
      fail("The table is not found.")
    }
    Thread.sleep(500)
  }

  it should "skip the table if skip settings contain the name of table" in {
    DB autoCommit { implicit session =>
      // Here is an example of flyway metadata table.
      SQL("""
        create table schema_version (
          version_rank int not null,
          installed_rank int not null,
          version varchar(50) not null,
          description varchar(200) not null,
          type varchar(20) not null,
          script varchar(1000) not null,
          checksum int,
          installed_by varchar(100) not null,
          installed_on timestamp default current_timestamp not null,
          execution_time int not null,
          success bit not null
        )
      """).execute.apply()
    }

    Model(url, username, password).table(null, "SCHEMA_VERSION").map { table =>
      val generator = new CodeGenerator(table)(using
        GeneratorConfig(
          srcDir = srcDir,
          packageName = "com.example",
          tableNamesToSkip = List("schema_version")
        )
      )
      generator.writeModelIfNonexistentAndUnskippable() should be(false)
    } getOrElse {
      fail("The table is not found.")
    }
    Thread.sleep(500)
  }

  it should "work fine for all tables defined above" in {
    val allTables = Model(url, username, password).allTables(null)
    allTables should have size 5
    allTables.foreach { table =>
      val generator = new CodeGenerator(table)(using
        GeneratorConfig(srcDir = srcDir, packageName = "com.example.alltables")
      )
      generator.writeModel()
    }
    Thread.sleep(500)
  }

  it should "retain underscores _[d] on table names with _[d]" in {
    DB autoCommit { implicit session =>
      SQL("""
        create table table_with_digits_1_2 (
          x_column_with_digits_3_4 varchar(30) not null,
          y_column_with_digits int,
          created_at timestamp not null
        )
      """).execute.apply()
    }

    Model(url, username, password).table(null, "TABLE_WITH_DIGITS_1_2").map {
      table =>
        val generator = new CodeGenerator(table)(using
          GeneratorConfig(
            srcDir = srcDir,
            packageName = "com.example",
            tableNamesToSkip = List("schema_version")
          )
        )
        generator.specAll()
        generator.writeModel()
    } getOrElse {
      fail("The table is not found.")
    }
  }

  it should "ok if specific Instant as time class" in {
    DB autoCommit { implicit session =>
      SQL("""
        create table table_with_instant (
          x_column_with_digits_3_4 varchar(30) not null,
          y_column_with_digits int,
          created_at timestamp not null
        )
      """).execute.apply()
    }

    Model(url, username, password).table(null, "TABLE_WITH_INSTANT").map {
      table =>
        val generator = new CodeGenerator(table)(using
          GeneratorConfig(
            srcDir = srcDir,
            packageName = "com.example",
            tableNamesToSkip = List("schema_version"),
            dateTimeClass = DateTimeClass.Instant
          )
        )
        generator.specAll()
        generator.writeModel()
    } getOrElse {
      fail("The table is not found.")
    }
  }

  it should "work fine with tableName_same_to_metatable" in {
    DB autoCommit { implicit session =>
      SQL("""
        create table "TABLES" (
          SCALIKEJDBC varchar(30) not null
        )
      """).execute.apply()
    }
    Model(url, username, password)
      .table("INFORMATION_SCHEMA", "TABLES", "MAPPER_GENERATOR_H2")
      .map { table =>
        if (table.allColumns.map(_.name).contains("SCALIKEJDBC")) {
          fail("the table generate extra column")
        }
        val generator = new CodeGenerator(table)(using
          GeneratorConfig(srcDir = srcDir, packageName = "com.example")
        )
        generator.writeModel()
      } getOrElse {
      fail("The table is not found.")
    }

    Model(url, username, password)
      .table("PUBLIC", "TABLES", "MAPPER_GENERATOR_H2")
      .map { table =>
        if (!table.allColumns.map(_.name).contains("SCALIKEJDBC")) {
          fail("the table does not generate specify column")
        }
        val generator = new CodeGenerator(table)(using
          GeneratorConfig(srcDir = srcDir, packageName = "com.example")
        )
        generator.writeModel()
      } getOrElse {
      fail("The table is not found.")
    }
    Thread.sleep(500)
  }
}
