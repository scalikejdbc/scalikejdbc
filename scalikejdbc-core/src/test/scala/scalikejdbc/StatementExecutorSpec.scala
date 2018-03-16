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
  }

}
