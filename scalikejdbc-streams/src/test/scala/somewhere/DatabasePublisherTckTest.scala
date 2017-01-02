package somewhere

import org.reactivestreams.Publisher
import org.reactivestreams.tck.{ PublisherVerification, TestEnvironment }
import org.testng.SkipException
import org.testng.annotations.{ AfterClass, BeforeClass }
import scalikejdbc._
import scalikejdbc.streams._
import somewhere.DatabasePublisherTckTest.User
import scala.concurrent.ExecutionContext.Implicits.global

class DatabasePublisherTckTest
    extends PublisherVerification[User](DatabasePublisherTckTest.environment)
    with TestDBSettings {

  private val tableName = "emp_DatabasePublisherTckTest" + System.currentTimeMillis()

  @BeforeClass
  def setupTable(): Unit = {
    initDatabaseSettings()
    initializeFixtures(tableName, 20000)
  }

  @AfterClass
  def teardownTable(): Unit = {
    dropTable(tableName)
  }

  override def createPublisher(elements: Long): Publisher[User] = {
    if (elements == Long.MaxValue) throw new SkipException("DatabasePublisher doesn't support infinite streaming.")

    DB readOnlyStream {
      SQL(s"select id from $tableName limit $elements").map(r => User(r.int("id"))).iterator
    }
  }

  override def createFailedPublisher(): Publisher[User] = {
    DB readOnlyStream {
      SQL(s"select id from $tableName").map[User](_ => throw new RuntimeException("this is failed publisher.")).iterator
    }
  }
}

object DatabasePublisherTckTest {

  val environment = new TestEnvironment(150L, false)

  case class User(id: Int)

}
