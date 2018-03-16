package scalikejdbc

import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import java.sql.PreparedStatement
import scala.reflect.runtime.{ universe => ru }

class StatementExecutorSpec extends FlatSpec with Matchers with MockitoSugar {

  behavior of "StatementExecutor"

  it should "be available" in {
    val underlying: PreparedStatement = null
    val template: String = ""
    val params: Seq[Any] = Nil
    val instance = new StatementExecutor(underlying, template, DBConnectionAttributes(), params)
    instance should not be null
  }

  it should "print sql string" in {
    val underlying: PreparedStatement = mock[PreparedStatement]
    val template: String = "select id, name from members where id = ? and name = ?"
    val params: Seq[Any] = Seq(1, "name1")
    val instance = new StatementExecutor(underlying, template, DBConnectionAttributes(), params)
    val runtimeMirror = ru.runtimeMirror(instance.getClass.getClassLoader)
    val instanceMirror = runtimeMirror.reflect(instance)
    val method = ru.typeOf[StatementExecutor].member(ru.TermName("sqlString")).asMethod
    val m = instanceMirror.reflectMethod(method)
    m.apply() should equal("select id, name from members where id = 1 and name = 'name1'")
  }

  it should "have PrintableQueryBuilder inside" in {
    {
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = "select id, name from users where id = ? and name = ?",
        settingsProvider = SettingsProvider.default,
        params = Seq(123, "Alice"))
      sql should equal("select id, name from users where id = 123 and name = 'Alice'")
    }

    {
      val sql = StatementExecutor.PrintableQueryBuilder.build(
        template = "select                       * from users where id = ?",
        settingsProvider = SettingsProvider.default,
        params = Seq(123))
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
        params = Seq("key"))
      sql should equal("select id, data from some_table where data ?? 'key'")
    }
  }

}
