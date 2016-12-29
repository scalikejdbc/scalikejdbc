package scalikejdbc.streams

import org.reactivestreams.Publisher
import org.reactivestreams.tck.{ PublisherVerification, TestEnvironment }
import org.testng.SkipException
import org.testng.annotations.{ AfterClass, BeforeClass }
import scalikejdbc._
import scalikejdbc.streams.DatabasePublisherTckTest.User

import scala.concurrent.ExecutionContext

class DatabasePublisherTckTest extends PublisherVerification[User](DatabasePublisherTckTest.environment) with TestDBSettings {
  implicit val executor = AsyncExecutor(ExecutionContext.global)

  @BeforeClass
  def setUpDb(): Unit = {
    openDB()

    loadFixtures { implicit session =>
      sql"drop table if exists users".execute().apply()
      sql"create table users(id INT)".execute().apply()

      for (i <- 0 to 9) {
        val delta = 2000
        val s = i * delta
        val batchParams: Seq[Seq[Any]] = ((s + 1) to (s + delta)).map(i => Seq(i))
        sql"insert into users(id) values (?)".batch(batchParams: _*).apply()
      }
    }
  }

  @AfterClass
  def teardownDb(): Unit = {
    closeDB()
  }

  override def createPublisher(elements: Long): Publisher[User] = {
    if (elements == Long.MaxValue) throw new SkipException("DatabasePublisher doesn't support infinite streaming.")
    db stream {
      sql"select id from users limit ${elements}".map(r => User(r.int("id"))).cursor
    }
  }

  override def createFailedPublisher(): Publisher[User] = {
    db stream {
      sql"select id from users".map[User](_ => throw new RuntimeException("this is failed publisher.")).cursor
    }
  }
}

object DatabasePublisherTckTest {
  val environment = new TestEnvironment(150L, false)

  case class User(id: Int)
}
