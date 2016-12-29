package scalikejdbc.streams

import org.scalatest._
import scalikejdbc._

import scala.concurrent.{ ExecutionContext, Promise }

class DatabasePublisherSpec extends AsyncFlatSpec with BeforeAndAfterAll with Matchers with LogSupport with TestDBSettings {
  implicit val executor = AsyncExecutor(ExecutionContext.global)

  override protected def beforeAll(): Unit = {
    openDB()

    loadFixtures { implicit session =>
      sql"drop table if exists users".execute().apply()
      sql"create table users(id INT)".execute().apply()

      for (i <- 1 to 10) {
        sql"insert into users(id) values ($i)".execute().apply()
      }
    }
  }

  override protected def afterAll(): Unit = {
    closeDB()
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

    val publisher = db stream {
      sql"select id from users".map(r => r.int("id")).cursor
    }

    publisher.subscribe(subscriber)

    promise.future.map(b => assert(b))
  }
}
