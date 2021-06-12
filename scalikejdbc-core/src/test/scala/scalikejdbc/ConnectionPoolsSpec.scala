package scalikejdbc

import java.util.Properties
import javax.sql.DataSource
import com.zaxxer.hikari.HikariDataSource
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConnectionPoolsSpec extends AnyFlatSpec with Matchers {

  behavior of "connection pools"

  val props = new Properties
  props.load(
    classOf[Settings].getClassLoader.getResourceAsStream("jdbc.properties")
  )

  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")
  val dataSourceClassName = props.getProperty("dataSourceClassName")

  Class.forName(driverClassName)

  it should "work" in {
    ConnectionPool.add(
      "dbcp",
      url,
      user,
      password,
      ConnectionPoolSettings(connectionPoolFactoryName = "commons-dbcp")
    )
    ConnectionPool.add(
      "bonecp",
      url,
      user,
      password,
      ConnectionPoolSettings(connectionPoolFactoryName = "bonecp")
    )

    val tableName = "connection_pool_" + System.currentTimeMillis
    NamedDB("dbcp").autoCommit { implicit s =>
      SQL(s"create table ${tableName} (id int, name varchar(256))").execute
        .apply()
      SQL(
        s"insert into ${tableName} (id, name) values (1, 'commons-dbcp')"
      ).update.apply()
      SQL(s"insert into ${tableName} (id, name) values (2, 'hikaricp')").update
        .apply()
      SQL(s"insert into ${tableName} (id, name) values (3, 'bonecp')").update
        .apply()
    }

    NamedDB("dbcp").readOnly { implicit s =>
      val count =
        SQL(s"select count(1) from ${tableName}").map(_.long(1)).single.apply()
      count should equal(Some(3))
    }

    NamedDB("bonecp").readOnly { implicit s =>
      val count =
        SQL(s"select count(1) from ${tableName}").map(_.long(1)).single.apply()
      count should equal(Some(3))
    }

    val dataSource: DataSource = {
      val ds = new HikariDataSource()
      ds.setMaximumPoolSize(15)
      ds.setDataSourceClassName(dataSourceClassName)
      ds.addDataSourceProperty("url", url)
      ds.addDataSourceProperty("user", user)
      ds.addDataSourceProperty("password", password)
      ds
    }
    ConnectionPool.add("hikaricp", new DataSourceConnectionPool(dataSource))

    NamedDB("hikaricp").readOnly { implicit s =>
      val count =
        SQL(s"select count(1) from ${tableName}").map(_.long(1)).single.apply()
      count should equal(Some(3))
    }

  }

}
