package somewhere

import org.reactivestreams.example.unicast.SyncSubscriber
import org.scalatest._
import org.slf4j.LoggerFactory
import scalikejdbc._
import scalikejdbc.streams._

import scala.concurrent.Promise

class DatabasePublisherSpec
    extends AsyncFlatSpec
    with BeforeAndAfterAll
    with Matchers
    with TestDBSettings {

  private lazy val log = LoggerFactory.getLogger(classOf[DatabasePublisherSpec])

  private val tableName = "emp_DatabasePublisherSpec" + System.currentTimeMillis()

  override protected def beforeAll(): Unit = {
    initDatabaseSettings()
    initializeFixtures(tableName, 2)
  }

  override protected def afterAll(): Unit = {
    dropTable(tableName)
  }

  "DatabasePublisher" should "be subscribed" in {
    val promise = Promise[Boolean]()
    val subscriber = new SyncSubscriber[Int] {
      override def foreach(element: Int): Boolean = {
        log.info(s"onNext element=$element")
        true
      }

      override def onError(t: Throwable): Unit = {
        super.onError(t)
        promise.tryFailure(t)
      }

      override def onComplete(): Unit = {
        super.onComplete()
        promise.trySuccess(true)
      }
    }

    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      SQL(s"select id from $tableName").map(r => r.int("id")).iterator
    }

    publisher.subscribe(subscriber)

    promise.future.map(b => assert(b))

  }

}
