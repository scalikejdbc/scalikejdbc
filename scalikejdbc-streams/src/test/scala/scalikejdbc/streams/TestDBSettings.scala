package scalikejdbc.streams

import scalikejdbc._

import scala.util.control.Exception.ignoring

trait TestDBSettings {

  protected def initDatabaseSettings(): Unit = {
    if (!ConnectionPool.isInitialized()) {
      // loading jdbc.properties
      val props = new java.util.Properties
      using(
        new java.io.FileInputStream(
          "scalikejdbc-core/src/test/resources/jdbc.properties"
        )
      ) { in => props.load(in) }
      // loading JDBC driver
      val driverClassName = props.getProperty("driverClassName")
      Class.forName(driverClassName)
      // preparing the connection pool settings
      val poolSettings = ConnectionPoolSettings(
        initialSize = 1,
        maxSize = 100,
        driverName = driverClassName
      )
      // JDBC settings
      val url = props.getProperty("url")
      val user = props.getProperty("user")
      val password = props.getProperty("password")
      ConnectionPool.singleton(url, user, password, poolSettings)
    }
  }

  protected def initializeFixtures(
    tableName: String,
    numberOfRecords: Int
  ): Unit = {
    implicit val settings =
      SettingsProvider.default.copy(loggingSQLAndTime = _.copy(enabled = false))
    DB.localTx { implicit session =>
      session.execute(
        s"create table $tableName (id integer primary key, name varchar(30))"
      )

      var i = 0
      val delta = 2000
      while ((numberOfRecords - 1) / delta >= i) {
        val s = i * delta
        val e =
          if ((s + delta) > numberOfRecords) numberOfRecords else s + delta
        val batchParams: Seq[Seq[Any]] = ((s + 1) to e).map(i => Seq(i))
        SQL(s"insert into $tableName (id) values (?)")
          .batch(batchParams*)
          .apply()
        i += 1
      }
    }
  }

  protected def dropTable(tableName: String): Unit = {
    ignoring(classOf[Throwable]) {
      DB autoCommit { _.execute(s"drop table $tableName") }
    }
  }

}
