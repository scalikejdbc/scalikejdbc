package scalikejdbc.streams

import org.scalatest._
import scalikejdbc._

import scala.concurrent.{ ExecutionContext, Promise }

class DatabasePublisherSpec
    extends AsyncFlatSpec
    with BeforeAndAfterAll
    with Matchers
    with LogSupport
    with TestDBSettings {

  private val tableName = "emp_DatabasePublisherSpec" + System.currentTimeMillis()

  implicit val executor = AsyncExecutor(ExecutionContext.global)

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
      override protected def whenNext(element: Int): Boolean = {
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
