package scalikejdbc.streams

import org.reactivestreams.Publisher
import org.reactivestreams.tck.{ PublisherVerification, TestEnvironment }
import org.testng.SkipException
import org.testng.annotations.{ AfterClass, BeforeClass }
import scalikejdbc._
import scalikejdbc.streams.DatabasePublisherTckTest.User

import scala.concurrent.ExecutionContext

class DatabasePublisherTckTest extends PublisherVerification[User](DatabasePublisherTckTest.environment) with TestDBSettings {
  private val tableName = "emp_DatabasePublisherTckTest" + System.currentTimeMillis()

  implicit val executor = AsyncExecutor(ExecutionContext.global)

  @BeforeClass
  def setupTable(): Unit = {
    openDB()
    initializeFixtures(tableName, 20000)
  }

  @AfterClass
  def teardownTable(): Unit = {
    dropTable(tableName)
  }

  override def createPublisher(elements: Long): Publisher[User] = {
    if (elements == Long.MaxValue) throw new SkipException("DatabasePublisher doesn't support infinite streaming.")
    DB stream {
      SQL(s"select id from $tableName limit $elements").map(r => User(r.int("id"))).cursor
    }
  }

  override def createFailedPublisher(): Publisher[User] = {
    DB stream {
      SQL(s"select id from $tableName").map[User](_ => throw new RuntimeException("this is failed publisher.")).cursor
    }
  }
}

object DatabasePublisherTckTest {
  val environment = new TestEnvironment(150L, false)

  case class User(id: Int)
}
