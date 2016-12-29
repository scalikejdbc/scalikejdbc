package scalikejdbc.streams

import scalikejdbc._

trait TestDBSettings {
  val settings: SettingsProvider = SettingsProvider.default.copy(loggingSQLAndTime = s => s.copy(singleLineMode = true))

  lazy val dbName = Symbol(this.getClass.getSimpleName)

  protected def openDB(): Unit = {
    val url = s"jdbc:h2:mem:streams_test_${dbName.name};LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0"
    val user = "user"
    val password = "password"
    val poolSettings = ConnectionPoolSettings(driverName = "org.h2.Driver")
    Class.forName(poolSettings.driverName)
    ConnectionPool.add(dbName, url, user, password, poolSettings)
  }

  protected def closeDB(): Unit = {
    ConnectionPool.close(dbName)
  }

  protected def db: NamedDB = {
    NamedDB(dbName, settings)
  }

  protected def loadFixtures(f: DBSession => Unit): Unit = {
    val settings = SettingsProvider.default.copy(loggingSQLAndTime = s => s.copy(enabled = false))
    NamedDB(dbName, settings).localTx(f)
  }
}
