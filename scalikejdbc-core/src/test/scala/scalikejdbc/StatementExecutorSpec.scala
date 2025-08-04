package scalikejdbc

import java.lang.reflect.Modifier
import java.sql.PreparedStatement
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StatementExecutorSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar {

  behavior of "StatementExecutor"

  it should "be available" in {
    val underlying: PreparedStatement = null
    val template: String = ""
    val params: collection.Seq[Any] = Nil
    val instance = new StatementExecutor(
      underlying,
      template,
      DBConnectionAttributes(),
      params
    )
    instance should not be null
  }

  it should "print sql string" in {
    val underlying: PreparedStatement = mock[PreparedStatement]
    val template: String =
      "select id, name from members where id = ? and name = ?"
    val params: collection.Seq[Any] = Seq(1, "name1")
    val instance = new StatementExecutor(
      underlying,
      template,
      DBConnectionAttributes(),
      params
    )
    val methods = classOf[StatementExecutor].getDeclaredMethods.filter { m =>
      (m.getName contains "sqlString") &&
      (Modifier isPublic m.getModifiers) &&
      (m.getReturnType == classOf[String]) &&
      (m.getParameterCount == 0)
    }.toList
    assert(methods.nonEmpty, methods)
    methods.foreach { m =>
      m.invoke(instance) should equal(
        "select id, name from members where id = 1 and name = 'name1'"
      )
    }
  }

  it should "have PrintableQueryBuilder inside" in {
    {
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = "select id, name from users where id = ? and name = ?",
        settingsProvider = SettingsProvider.default,
        params = Seq(123, "Alice")
      )
      sql should equal(
        "select id, name from users where id = 123 and name = 'Alice'"
      )
    }

    {
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = "select                       * from users where id = ?",
        settingsProvider = SettingsProvider.default,
        params = Seq(123)
      )
      sql should equal("select * from users where id = 123")
    }

    // Handle escaped question marks when printing queries.
    //  Postgres has some operators for working with JSON data that include `?`.
    //    As a result, its JDBC driver treats `??` as an escaped question mark,
    //  rather than two parameter placeholders, so that these operators can be used
    //  in prepared statements.  Scalikejdbc mostly handles this correctly except
    //  that the queries it logs for debugging purposes do not correctly account
    //  for this and instead treat `??` as two parameters.  This PR fixes that.
    //
    //  I don't think this will do any harm when used with non-Postgres JDBC
    //    drivers because `??` is not sensical as two parameter placeholders anyway.
    {
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = "select id, data from some_table where data ?? ?",
        settingsProvider = SettingsProvider.default,
        params = Seq("key")
      )
      sql should equal("select id, data from some_table where data ?? 'key'")
    }

    /*
      [info] - should have PrintableQueryBuilder inside *** FAILED ***
      [info]   java.lang.IllegalArgumentException: Illegal group reference
      [info]   at java.util.regex.Matcher.appendReplacement(Matcher.java:857)
      [info]   at scala.util.matching.Regex$Replacement.replace(Regex.scala:897)
      [info]   at scala.util.matching.Regex$Replacement.replace$(Regex.scala:897)
      [info]   at scala.util.matching.Regex$MatchIterator$$anon$1.replace(Regex.scala:875)
      [info]   at scala.util.matching.Regex.$anonfun$replaceAllIn$1(Regex.scala:505)
      [info]   at scala.collection.Iterator.foreach(Iterator.scala:929)
      [info]   at scala.collection.Iterator.foreach$(Iterator.scala:929)
      [info]   at scala.collection.AbstractIterator.foreach(Iterator.scala:1417)
      [info]   at scala.util.matching.Regex.replaceAllIn(Regex.scala:505)
      [info]   at scalikejdbc.StatementExecutor$PrintableQueryBuilder.$anonfun$build$2(StatementExecutor.scala:87)
      [info]   ...
     */
    {
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = "select * from users where foo = ? and bar = ?",
        settingsProvider = SettingsProvider.default,
        params = Seq("foo$", "^bar$\\+$")
      )
      sql should equal(
        "select * from users where foo = 'foo$' and bar = '^bar$\\+$'"
      )
    }

    /*
      [info] scalikejdbc.StatementExecutorSpec *** ABORTED ***
      [info]   java.lang.InternalError: Malformed class name
      [info]   at java.lang.Class.getSimpleName(Class.java:1330)
      [info]   at java.lang.Class.getCanonicalName(Class.java:1399)
      [info]   at scalikejdbc.StatementExecutor$PrintableQueryBuilder.normalize$1(StatementExecutor.scala:65)
      [info]   at scalikejdbc.StatementExecutor$PrintableQueryBuilder.toPrintable$1(StatementExecutor.scala:80)
      [info]   at scalikejdbc.StatementExecutor$PrintableQueryBuilder.$anonfun$build$3(StatementExecutor.scala:110)
      [info]   at scala.util.matching.Regex.$anonfun$replaceAllIn$1(Regex.scala:504)
      [info]   at scala.collection.Iterator.foreach(Iterator.scala:937)
      [info]   at scala.collection.Iterator.foreach$(Iterator.scala:937)
      [info]   at scala.collection.AbstractIterator.foreach(Iterator.scala:1425)
      [info]   at scala.util.matching.Regex.replaceAllIn(Regex.scala:504)
      [info]   ...
     */
    {
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = "select * from users where bar = ?",
        settingsProvider = SettingsProvider.default,
        params = Seq(Foo.Bar)
      )
      sql should equal("select * from users where bar = Bar")
    }
  }

  // #968 Embedded quotes in SQL statements are removed in debug logging
  it should "have PrintableQueryBuilder resolving #968" in {

    {
      val statement =
        "select id, name from users where id = 123 and name = 'Alice'"
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = statement,
        settingsProvider = SettingsProvider.default,
        params = Seq.empty
      )
      sql should equal(statement)
    }
    {
      val statement =
        "select id, name from users where first_name = 'Bob' and id = 123 and last_name = 'Marley' and code = 777"
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = statement,
        settingsProvider = SettingsProvider.default,
        params = Seq.empty
      )
      sql should equal(statement)
    }

    // escaped quotes
    {
      val statement =
        "select id, name from users where name = 'Bob' and venue = 'Bob'' house' and id = 123"
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = statement,
        settingsProvider = SettingsProvider.default,
        params = Seq.empty
      )
      sql should equal(statement)
    }

    // invalid statements
    {
      {
        val sql = StatementExecutor.PrintableQueryBuilder.build(
          template =
            "select id, name from users where name = ''Bob and id = 123",
          settingsProvider = SettingsProvider.default,
          params = Seq.empty
        )
        sql should equal(
          "select id, name from users where name = ''Bob and id = 123"
        )
      }
      {
        val sql = StatementExecutor.PrintableQueryBuilder.build(
          template =
            "select id, name from users where name = 'Bob and id = 123",
          settingsProvider = SettingsProvider.default,
          params = Seq.empty
        )
        sql should equal(
          "select id, name from users where name = 'Bob and id = 123'"
        )
      }
    }
  }

  it should "get stackTraceInformation" in {
    val executor = StatementExecutor(
      underlying = mock[PreparedStatement],
      template = "",
      connectionAttributes = DBConnectionAttributes(),
      settingsProvider = SettingsProvider.default.copy(
        loggingSQLAndTime = Function.const(LoggingSQLAndTimeSettings())
      )
    )
    val Some(method) =
      classOf[StatementExecutor].getMethods
        .find { m =>
          (m.getName contains "stackTraceInformation") &&
          (m.getReturnType == classOf[String]) &&
          (m.getParameterCount == 0) &&
          !Modifier.isStatic(m.getModifiers)
        }
    val stackTraceInfo = method.invoke(executor).asInstanceOf[String]
    assert(stackTraceInfo.contains("  [Stack Trace]"))
  }

  it should "handle large bind values efficiently (issue #2453)" in {
    val bindValues = (1 to 50000).toSeq
    val placeholders = bindValues.map(_ => "?").mkString(", ")
    val template = s"INSERT INTO table (column1) VALUES ($placeholders)"

    val startTime = System.currentTimeMillis()
    val sql = StatementExecutor.PrintableQueryBuilder.build(
      template = template,
      settingsProvider = SettingsProvider.default,
      params = bindValues
    )
    val endTime = System.currentTimeMillis()
    val duration = endTime - startTime

    // Before the fix, it takes ~633ms for 50k values.
    duration should be < 500L
  }

  object Foo {
    case object Bar
  }
}
