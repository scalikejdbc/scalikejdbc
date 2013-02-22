package scalikejdbc.config

import org.scalatest.FunSpec
import org.scalatest.matchers._
import scalikejdbc._
import com.typesafe.config.{ Config => TypesafeConfig, _ }

class TypesafeConfigReaderSpec extends FunSpec with ShouldMatchers {

  describe("TypesafeConfigReader") {

    describe ("#readJDBCSettings") {

      it ("should read configuration and return as JDBCSettings") {
        val expected = JDBCSettings("org.h2.Driver", "jdbc:h2:mem:test1", "sa", "secret")
        TypesafeConfigReader.readJDBCSettings() should be (expected)
      }

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
          "password" -> "secret",
          "poolInitialSize" -> "1",
          "poolMaxSize" -> "2",
          "poolValidationQuery" -> "select 1 as foo"
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
      val expected = List("default", "foo", "bar", "baz").sorted
      TypesafeConfigReader.dbNames.sorted should be (expected)
    }

    describe ("#readConnectionPoolSettings") {

      it ("should read configuration and return as ConnectionPoolSettings") {
        val expected = ConnectionPoolSettings(5, 7, "select 1 as one")
        TypesafeConfigReader.readConnectionPoolSettings() should be (expected)
      }

      it ("should read configuration for foo db and return as ConnectionPoolSettings") {
        val expected = ConnectionPoolSettings(1, 2, "select 1 as foo")
        TypesafeConfigReader.readConnectionPoolSettings('foo) should be (expected)
      }

      it ("should read configuration for bar db and return as ConnectionPoolSettings") {
        val expected = ConnectionPoolSettings(2, 3, "select 1 as bar")
        TypesafeConfigReader.readConnectionPoolSettings('bar) should be (expected)
      }

      describe ("When an unknown database name is passed") {
        it ("throws Configuration Exception") {
          intercept[ConfigurationException] {
            TypesafeConfigReader.readConnectionPoolSettings('unknown)
          }
        }
      }

    }

    val emptyConfigReader = new TypesafeConfigReader with Config {
      override lazy val config: TypesafeConfig = ConfigFactory.load("empty.conf")
    }

    val badConfigReader = new TypesafeConfigReader with Config {
      override lazy val config: TypesafeConfig = ConfigFactory.load("application-bad.conf")
    }

    val badConfigReaderLogEnabled = new TypesafeConfigReader with Config {
      override lazy val config: TypesafeConfig = ConfigFactory.load("application-bad-logenabled.conf")
    }

    describe ("#loadGlobalSettings") {

      it ("should load global settings") {
        TypesafeConfigReader.loadGlobalSettings()
      }

      describe ("When the format of config file is bad") {
        it ("should not throw Exception") {
          badConfigReader.loadGlobalSettings()
          badConfigReaderLogEnabled.loadGlobalSettings()
        }
      }

      describe ("When the config file is empty") {
        it ("should not throw Exception") {
          emptyConfigReader.loadGlobalSettings()
        }
      }
    }

  }

}
