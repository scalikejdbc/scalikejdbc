package scalikejdbc.config

import scalikejdbc._

/**
 * DB configurator
 */
trait DBs { self: TypesafeConfigReader & TypesafeConfig & EnvPrefix =>

  def setup(dbName: String = ConnectionPool.DEFAULT_NAME): Unit = {
    val JDBCSettings(url, user, password, driver) = readJDBCSettings(dbName)
    val cpSettings = readConnectionPoolSettings(dbName)
    if (driver != null && driver.trim.nonEmpty) {
      Class.forName(driver)
    }
    ConnectionPool.add(dbName, url, user, password, cpSettings)
  }

  def setupAll(): Unit = {
    loadGlobalSettings()
    dbNames.foreach { dbName => setup(dbName) }
  }

  def close(dbName: String = ConnectionPool.DEFAULT_NAME): Unit = {
    ConnectionPool.close(dbName)
  }

  def closeAll(): Unit = {
    ConnectionPool.closeAll()
  }

}

/**
 * Default DB setup executor
 */
object DBs
  extends DBs
  with TypesafeConfigReader
  with StandardTypesafeConfig
  with NoEnvPrefix
