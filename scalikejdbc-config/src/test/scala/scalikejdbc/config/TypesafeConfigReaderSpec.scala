package scalikejdbc.config

import org.scalatest.FunSpec
import org.scalatest.matchers._
import scalikejdbc._

class TypesafeConfigReaderSpec extends FunSpec with ShouldMatchers {

  def fixture = new {

  }

  describe("TypesafeConfigReader") {

    describe ("#readJDBCSettings") {

      it ("should read configuration by db name and return as JDBCSettings") {
        val expected = JDBCSettings("org.h2.Driver", "jdbc:h2:mem:test2", "sa", "secret")
        TypesafeConfigReader.readJDBCSettings('foo) should be (expected)
      }

      describe ("When an unknown database name is passed") {
        it ("throws Configuration Exception") {
          intercept[ConfigurationException] {
            TypesafeConfigReader.readJDBCSettings('unknown)
          }
        }
      }

    }

    describe ("#readAsMap") {
      it ("should read configuration by db name and return as Map") {
        val expected = Map(
          "driver" -> "org.h2.Driver",
          "url" -> "jdbc:h2:mem:test2",
          "user" -> "sa",
          "password" -> "secret"
        )
        TypesafeConfigReader.readAsMap('foo) should be (expected)
      }

      describe ("When an unknown database name is passed") {
        it ("throws Configuration Exception") {
          intercept[ConfigurationException] {
            TypesafeConfigReader.readAsMap('unknown)
          }
        }
      }

    }

    it ("should get db names") {
      val expected = List("default", "foo", "bar").sorted
      TypesafeConfigReader.dbNames.sorted should be (expected)
    }
  }

}
