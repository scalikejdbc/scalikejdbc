package somewhere

import org.reactivestreams.Publisher
import org.reactivestreams.tck.{ PublisherVerification, TestEnvironment }
import org.scalatestplus.testng.TestNGSuiteLike
import org.testng.SkipException
import org.testng.annotations.{ AfterClass, BeforeClass, Test }
import scalikejdbc._
import scalikejdbc.streams._
import somewhere.DatabasePublisherTckTest.User

import scala.concurrent.ExecutionContext.Implicits.global

class DatabasePublisherTckTest(
  env: TestEnvironment,
  publisherShutdownTimeout: Long
) extends PublisherVerification[User](env, publisherShutdownTimeout)
  with TestDBSettings
  with TestNGSuiteLike {

  def this() = {
    this(DatabasePublisherTckTest.environment, 1000)
  }

  private val tableName =
    "emp_DatabasePublisherTckTest" + System.currentTimeMillis()

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
    if (elements == Long.MaxValue)
      throw new SkipException(
        "DatabasePublisher doesn't support infinite streaming."
      )

    DB readOnlyStream {
      SQL(s"select id from $tableName limit $elements")
        .map(r => User(r.int("id")))
        .iterator()
    }
  }

  override def createFailedPublisher(): Publisher[User] = {
    DB readOnlyStream {
      SQL(s"select id from $tableName")
        .map[User](_ => throw new RuntimeException("this is failed publisher."))
        .iterator()
    }
  }

  /*
   * Additional Tests
   */

  // If there is 0 record at the first data fetch, complete the streaming and should not leave the DBConnection unnecessarily open.
  // test for https://github.com/scalikejdbc/scalikejdbc/pull/614/commits/0c1c120272fe49cde399a7e57a42f78701d5f830
  @Test
  def optional_spec105_shouldSignalOnCompleteWithoutRequestWhenResultSetIsEmptyAtFirstFetch()
    : Unit = {
    optionalActivePublisherTest(
      0,
      true,
      (pub: Publisher[User]) => {
        val sub = env.newManualSubscriber(pub)
        sub.expectCompletion()
        sub.expectNone()
      }
    )
  }

  /*
   * Override Tests
   *
   * If subscribe from DatabasePublisher, it have to complete the streaming and close the DB connection.
   * The original TCK,
   * - required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements
   * - optional_spec111_maySupportMultiSubscribe
   * will not cancel the streaming after testing. Therefore, we override these.
   */

  // Verifies rule: https://github.com/reactive-streams/reactive-streams-jvm#1.1
  // see also: https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/tck/src/main/java/org/reactivestreams/tck/PublisherVerification.java#L195-L216
  @Test
  override def required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements()
    : Unit = {
    activePublisherTest(
      5,
      false,
      (pub: Publisher[User]) => {
        val sub = env.newManualSubscriber(pub)
        sub.expectNone(
          String.format(
            "Publisher %s produced value before the first `request`: ",
            pub
          )
        )
        sub.request(1)
        sub.nextElement(
          String.format(
            "Publisher %s produced no element after first `request`",
            pub
          )
        )
        sub.expectNone(
          String.format("Publisher %s produced unrequested: ", pub)
        )
        sub.request(1)
        sub.request(2)
        sub.nextElements(
          3,
          env.defaultTimeoutMillis,
          String.format(
            "Publisher %s produced less than 3 elements after two respective `request` calls",
            pub
          )
        )
        sub.expectNone(
          String.format("Publisher %sproduced unrequested ", pub)
        )

        // clean up for Connection release
        sub.cancel()
      }
    )
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams-jvm#1.11
  // see also: https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/tck/src/main/java/org/reactivestreams/tck/PublisherVerification.java#L540-L552
  @Test
  override def optional_spec111_maySupportMultiSubscribe(): Unit = {
    optionalActivePublisherTest(
      1,
      false,
      (pub: Publisher[User]) => {
        val sub1 = env.newManualSubscriber(pub)
        val sub2 = env.newManualSubscriber(pub)
        env.verifyNoAsyncErrors()

        // clean up for Connection release
        sub1.cancel()
        sub2.cancel()
      }
    )
  }
}

object DatabasePublisherTckTest {

  val timeoutMillis = 150L
  val environment = new TestEnvironment(timeoutMillis, timeoutMillis, false)

  case class User(id: Int)

}
