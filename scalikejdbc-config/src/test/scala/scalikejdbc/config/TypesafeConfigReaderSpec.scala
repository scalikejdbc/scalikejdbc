package scalikejdbc.config

import org.scalatest._
import scalikejdbc._
import com.typesafe.config._

class TypesafeConfigReaderSpec extends FunSpec with Matchers {

  val play24ConfigReader = new TypesafeConfigReader with TypesafeConfig {
    override lazy val config: Config = ConfigFactory.load("application-play24.conf")
  }

  val emptyConfigReader = new TypesafeConfigReader with TypesafeConfig {
    override lazy val config: Config = ConfigFactory.load("empty.conf")
  }

  val badConfigReader = new TypesafeConfigReader with TypesafeConfig {
    override lazy val config: Config = ConfigFactory.load("application-bad.conf")
  }

  val badConfigReaderLogEnabled = new TypesafeConfigReader with TypesafeConfig {
    override lazy val config: Config = ConfigFactory.load("application-bad-logenabled.conf")
  }

  val configReaderServerTimeZoneSpecified = new TypesafeConfigReader with TypesafeConfig {
    override lazy val config: Config = ConfigFactory.load("application-server-time-zone-specified.conf")
  }

  describe("TypesafeConfigReader") {

    describe("#readJDBCSettings") {

      it("should read configuration and return as JDBCSettings") {
        val expected = JDBCSettings("jdbc:h2:mem:test1", "sa", "secret", "org.h2.Driver")
        TypesafeConfigReader.readJDBCSettings() should be(expected)
      }

      it("should read configuration by db name and return as JDBCSettings") {
        val expected = JDBCSettings("jdbc:h2:mem:test2", "sa", "secret", "org.h2.Driver")
        TypesafeConfigReader.readJDBCSettings('foo) should be(expected)
      }

      describe("When user and password is not specified in application.conf") {
        it("should return JDBCSettings the user and password of which is null") {
          val expected = JDBCSettings("jdbc:h2:mem:test4", null, null, "org.h2.Driver")
          TypesafeConfigReader.readJDBCSettings('baz) should be(expected)
        }
      }

      describe("When configuration file is Play 2.4 or higher compatible") {
        it("should return JDBCSettings as expected") {
          val expected = JDBCSettings("jdbc:h2:mem:play24", "un", "p", "org.h2.Driver")
          play24ConfigReader.readJDBCSettings() should be(expected)
        }
      }

      describe("When configuration file is empty") {
        it("throws Configuration Exception") {
          intercept[ConfigurationException] {
            emptyConfigReader.readJDBCSettings('foo) should be(None)
          }
        }
      }

      describe("When an unknown database name is passed") {
        it("throws Configuration Exception") {
          intercept[ConfigurationException] {
            TypesafeConfigReader.readJDBCSettings('unknown)
          }
        }
      }

    }

    describe("#readAsMap") {
      it("should read configuration by db name and return as Map") {
        val expected = Map(
          "driver" -> "org.h2.Driver",
          "url" -> "jdbc:h2:mem:test2",
          "user" -> "sa",
          "password" -> "secret",
          "poolInitialSize" -> "1",
          "poolMaxSize" -> "2",
          "connectionTimeoutMillis" -> "2000",
          "poolConnectionTimeoutMillis" -> "1000",
          "poolValidationQuery" -> "select 1 as foo",
          "poolWarmUpTimeMillis" -> "10"
        )
        TypesafeConfigReader.readAsMap('foo) should be(expected)
      }

      describe("When an unknown database name is passed") {
        it("throws Configuration Exception") {
          intercept[ConfigurationException] {
            TypesafeConfigReader.readAsMap('unknown)
          }
        }
      }

      it("should read configuration by env and return as Map") {
        val expected = Map(
          "driver" -> "org.h2.Driver",
          "url" -> "jdbc:h2:mem:dev",
          "user" -> "dev",
          "password" -> "secret"
        )
        val configReader = new TypesafeConfigReaderWithEnv("dev")
        configReader.readAsMap() should be(expected)
      }
      it("should read configuration by env(prod) and return as Map") {
        val expected = Map(
          "driver" -> "org.h2.Driver3",
          "url" -> "jdbc:h2:mem:prod",
          "user" -> "prod",
          "password" -> "secret3"
        )
        val configReader = new TypesafeConfigReaderWithEnv("prod")
        configReader.readAsMap() should be(expected)
      }
      it("should read configuration by env and db name and return as Map") {
        val expected = Map(
          "poolValidationQuery" -> "select 1 as foo",
          "poolConnectionTimeoutMillis" -> "1000",
          "connectionTimeoutMillis" -> "2000",
          "poolInitialSize" -> "1",
          "poolMaxSize" -> "2",
          "driver" -> "org.h2.Driver",
          "url" -> "jdbc:h2:mem:dev-foo",
          "user" -> "dev-foo",
          "password" -> "secret2",
          "poolWarmUpTimeMillis" -> "10"
        )
        val configReader = new TypesafeConfigReaderWithEnv("dev")
        configReader.readAsMap('foo) should be(expected)
      }

      it("should read top level configuration and return as Map") {
        val expected = Map(
          "driver" -> "org.h2.Driver",
          "url" -> "jdbc:h2:mem:topLevelDefaults",
          "user" -> "xxx",
          "password" -> "yyy"
        )
        TypesafeConfigReader.readAsMap('topLevelDefaults) should be(expected)
      }

      it("should read configuration by env and top level defaults and db name and return as Map") {
        val expected = Map(
          "driver" -> "org.h2.Driver",
          "url" -> "jdbc:h2:mem:topLevelDefaults",
          "user" -> "app",
          "password" -> "password"
        )
        val configReader = new TypesafeConfigReaderWithEnv("prod")
        configReader.readAsMap('topLevelDefaults) should be(expected)
      }

    }

    it("should get db names") {
      val expected = List("default", "foo", "bar", "baz", "topLevelDefaults").sorted
      TypesafeConfigReader.dbNames.sorted should be(expected)
    }

    describe("#readConnectionPoolSettings") {

      it("should read configuration and return as ConnectionPoolSettings") {
        val expected = ConnectionPoolSettings(5, 7, 1000L, "select 1 as one", "commons-dbcp", "org.h2.Driver", 10L)
        TypesafeConfigReaderWithEnv("settings").readConnectionPoolSettings() should be(expected)
      }

      it("should read configuration for foo db and return as ConnectionPoolSettings") {
        val expected = ConnectionPoolSettings(1, 2, 1000L, "select 1 as foo", null, "org.h2.Driver", 10L)
        TypesafeConfigReader.readConnectionPoolSettings('foo) should be(expected)
      }

      it("should read configuration for bar db and return as ConnectionPoolSettings") {
        val expected = ConnectionPoolSettings(2, 3, 1000L, "select 1 as bar", null, "org.h2.Driver", 10L)
        TypesafeConfigReader.readConnectionPoolSettings('bar) should be(expected)
      }

      describe("When an unknown database name is passed") {
        it("throws Configuration Exception") {
          intercept[ConfigurationException] {
            TypesafeConfigReader.readConnectionPoolSettings('unknown)
          }
        }
      }

    }

    describe("#loadGlobalSettings") {

      it("should load global settings") {
        TypesafeConfigReader.loadGlobalSettings()
      }

      it("should load serverTimeZone") {
        configReaderServerTimeZoneSpecified.loadGlobalSettings()
        GlobalSettings.serverTimeZone.get.getID should equal("AST")
      }

      describe("When the format of config file is bad") {
        it("should not throw Exception") {
          badConfigReader.loadGlobalSettings()
          badConfigReaderLogEnabled.loadGlobalSettings()
        }
      }

      describe("When the config file is empty") {
        it("should not throw Exception") {
          emptyConfigReader.loadGlobalSettings()
        }
      }
    }

  }

}
