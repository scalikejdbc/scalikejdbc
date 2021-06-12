package scalikejdbc

import org.slf4j.LoggerFactory

import scala.util.control.NonFatal
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PostgreSQL_JSON_Objects_Spec
  extends AnyFlatSpec
  with Matchers
  with DBSettings
  with SQLInterpolation {

  behavior of "Handling of PostgreSQL JSON objects"

  val logger = LoggerFactory.getLogger(classOf[PostgreSQL_JSON_Objects_Spec])

  private def isTestWithPostgreSQL(): Boolean = {
    driverClassName == "org.postgresql.Driver"
  }

  it should "perform DDLs/DMLs" in {
    if (isTestWithPostgreSQL()) {
      implicit val session = AutoSession
      val tableName = sqls"json_data_table"
      // preparation
      try {
        sql"drop table $tableName".execute.apply()
      } catch {
        case NonFatal(_) =>
      }
      sql"create table $tableName (c1 json, c2 jsonb, c3 json not null, c4 jsonb not null)".execute
        .apply()
      sql"create index ${tableName}_idx1 on $tableName using gin (c4)".execute
        .apply()

      {
        // You can pass JSON string data as a normal parameter to prepared statement.
        // But it requires ::json or ::jsonb afterwards in a SQL statement.
        val json =
          """
            |{
            |  "name": "Alice",
            |  "birthYear": 1995
            |}
            |""".stripMargin
        sql"insert into $tableName (c3, c4) values ($json::json, $json::jsonb)".update
          .apply()
      }
      {
        // Embedding JSON as a SQLSyntax, it requires enclosing the JSON part with single quotes
        val json =
          sqls"""
            |'
            |{
            |  "name": "Bob",
            |  "birthYear": 1992
            |}
            |'
            |""".stripMargin
        // Having ::json, ::jsonb is not mandatory for this case.
        sql"insert into $tableName (c3, c4) values ($json, $json)".update
          .apply()
        sql"insert into $tableName (c3, c4) values ($json::json, $json::jsonb)".update
          .apply()
      }
      val results: Seq[Map[String, Any]] =
        sql"select c1, c2, c3, c4 from $tableName".toMap.list.apply()
      logger.debug(results.toString)
      results.size should be > 0
    } else {
      logger.info(
        s"Skipped because the current JDBC driver is $driverClassName"
      )
    }
  }

  it should "perform json operators" in {
    // https://www.postgresql.org/docs/9.5/functions-json.html
    if (isTestWithPostgreSQL()) {
      val tableName = sqls"json_operators_table"

      DB.autoCommit { implicit s =>
        // preparation
        try {
          sql"drop table $tableName".execute.apply()
        } catch {
          case NonFatal(_) =>
        }
        sql"create table $tableName (id serial, config jsonb not null)".execute
          .apply()

        val config1 =
          """
            |{
            |  "host": "localhost",
            |  "port": 5432,
            |  "credentials": {
            |    "api-1": "12345",
            |    "api-2": "abcdef"
            |  }
            |}
            |""".stripMargin
        val config2 =
          """
            |{
            |  "host": "somewhere-beautiful.com",
            |  "port": 443,
            |  "applications": ["frontend", "backend-api", "database"]
            |}
            |""".stripMargin
        sql"insert into $tableName (config) values ($config1::jsonb)".update
          .apply()
        sql"insert into $tableName (config) values ($config2::jsonb)".update
          .apply()
      }

      DB.readOnly { implicit s =>
        val apps: Seq[Option[String]] =
          sql"select config::jsonb->'applications' apps from $tableName order by id"
            .map(_.getOpt[String]("apps"))
            .list
            .apply()
        apps should equal(
          Seq(None, Some("""["frontend", "backend-api", "database"]"""))
        )

        val credentials: Seq[Option[String]] =
          sql"select config::jsonb#>'{credentials,api-2}' c from $tableName order by id"
            .map(_.getOpt[String]("c"))
            .list
            .apply()
        credentials should equal(Seq(Some("\"abcdef\""), None))

        val credentials2: Seq[Option[String]] =
          sql"select config::jsonb#>>'{credentials,api-2}' c from $tableName order by id"
            .map(_.getOpt[String]("c"))
            .list
            .apply()
        credentials2 should equal(Seq(Some("abcdef"), None))

        val hosts: Seq[String] =
          sql"select config::jsonb->'host' host from $tableName order by id"
            .map(_.get[String]("host"))
            .list
            .apply()
        hosts should equal(Seq("\"localhost\"", "\"somewhere-beautiful.com\""))

        val hostsAsText: Seq[String] =
          sql"select config::jsonb->>'host' host from $tableName order by id"
            .map(_.get[String]("host"))
            .list
            .apply()
        hostsAsText should equal(Seq("localhost", "somewhere-beautiful.com"))
      }

    } else {
      logger.info(
        s"Skipped because the current JDBC driver is $driverClassName"
      )
    }
  }

}
