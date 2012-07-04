import org.scalatest._
import org.scalatest.matchers._

import scalikejdbc._
import mapper.{ GeneratorTemplate, GeneratorConfig, ARLikeTemplateGenerator, Model }

class MapperGeneratorSpec extends FlatSpec with ShouldMatchers {

  Class.forName("org.h2.Driver")

  val url = "jdbc:h2:file:db/scalikejdbc-mapper-generator"
  val username = "sa"
  val password = ""
  ConnectionPool.singleton(url, username, password)

  it should "work fine with member" in {
    DB autoCommit { implicit session =>
      try {
        SQL("select count(1) from member").map(rs => rs).list.apply()
      } catch {
        case e =>
          e.printStackTrace()
          SQL("""
            create table member (
              id int generated always as identity,
              name varchar(30) not null,
              description varchar(1000),
              birthday date,
              created_at timestamp not null,
              primary key(id)
            )
            """).execute.apply()
      }
    }

    Model(url, username, password).table(null, "MEMBER").map {
      table =>
        val generator1 = ARLikeTemplateGenerator(table)(GeneratorConfig(
          srcDir = "scalikejdbc-mapper-generator/src/test/scala",
          template = GeneratorTemplate("executableSQL"),
          packageName = "com.example"
        ))
        println(generator1.generateAll())
        generator1.writeFileIfNotExist()
        val generator2 = ARLikeTemplateGenerator(table)(GeneratorConfig(
          srcDir = "scalikejdbc-mapper-generator/src/test/scala",
          template = GeneratorTemplate("placeHolderSQL"),
          packageName = "com.example.placeholder"
        ))
        println(generator2.generateAll())
        generator2.writeFileIfNotExist()
        val generator3 = ARLikeTemplateGenerator(table)(GeneratorConfig(
          srcDir = "scalikejdbc-mapper-generator/src/test/scala",
          template = GeneratorTemplate("anormSQL"),
          packageName = "com.example.anorm"
        ))
        println(generator3.generateAll())
        generator3.writeFileIfNotExist()
    } getOrElse {
      fail("The table is not found.")
    }
    Thread.sleep(500)
  }

  it should "work fine with large table" in {

    DB autoCommit { implicit session =>
      try {
        SQL("select count(1) from un_normalized").map(rs => rs).list.apply()
      } catch {
        case e =>
          e.printStackTrace()
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
/*
            v_11 CLOB(30K),
            v_12 LONGVARCHAR,
            v_13 BINARY(10) not null,
            v_14 VARBINARY(10) not null,
            v_15 BLOB(30K),
 */
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
            v_21 OTHER,
            v_22 BOOLEAN not null,
            v_23 REAL not null,
            v_24 FLOAT not null,
            created_at timestamp not null,
            primary key(id)
          )
          """).execute.apply()
      }
    }

    Model(url, username, password).table(null, "UN_NORMALIZED").map {
      table =>
        val generator = ARLikeTemplateGenerator(table)(GeneratorConfig(
          srcDir = "scalikejdbc-mapper-generator/src/test/scala",
          packageName = "com.example"
        ))
        generator.writeFileIfNotExist()
    } getOrElse {
      fail("The table is not found.")
    }

    Thread.sleep(500)
  }

  it should "work fine with without_pk" in {
    DB autoCommit { implicit session =>
      try {
        SQL("select count(1) from without_pk").map(rs => rs).list.apply()
      } catch {
        case e =>
          e.printStackTrace()
          SQL("""
            create table without_pk (
              aaa varchar(30) not null,
              bbb int,
              created_at timestamp not null
            )
            """).execute.apply()
      }
    }

    Model(url, username, password).table(null, "WITHOUT_PK").map {
      table =>
        val generator = ARLikeTemplateGenerator(table)(GeneratorConfig(
          srcDir = "scalikejdbc-mapper-generator/src/test/scala",
          packageName = "com.example"
        ))
        generator.writeFileIfNotExist()
    } getOrElse {
      fail("The table is not found.")
    }
    Thread.sleep(500)
  }

}
