package somewhere

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.concurrent._

import org.reactivestreams.example.unicast.{ AsyncSubscriber, SyncSubscriber }
import org.scalatest._
import org.slf4j.LoggerFactory
import scalikejdbc._
import scalikejdbc.streams._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Promise
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class DatabasePublisherSpec
  extends AsyncFlatSpec
  with BeforeAndAfterAll
  with Matchers
  with TestDBSettings {

  private lazy val log = LoggerFactory.getLogger(classOf[DatabasePublisherSpec])

  private val tableName =
    "emp_DatabasePublisherSpec" + System.currentTimeMillis()
  private val totalRows = 100

  override protected def beforeAll(): Unit = {
    initDatabaseSettings()
    initializeFixtures(tableName, totalRows)
  }

  override protected def afterAll(): Unit = {
    dropTable(tableName)
  }

  behavior of "DatabasePublisher"

  // ------------------------------------------
  // SyncSubscriber
  // ------------------------------------------

  it should "be subscribed by SyncSubscriber" in {
    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      SQL(s"select id from $tableName").map(_.int("id")).iterator()
    }

    val consumedCountPromise: Promise[Int] = Promise[Int]()
    val subscriber: SyncSubscriber[Int] = new SyncSubscriber[Int] {
      private[this] val consumedCount = new AtomicInteger(0)

      override def whenNext(element: Int): Boolean = {
        val consumed = consumedCount.incrementAndGet()
        log.info(s"foreach element: $element, consumed: $consumed")
        true
      }
      override def onError(error: Throwable): Unit = {
        super.onError(error)
        log.info(s"Error - ${error}, consumed: ${consumedCount.get()}")
        consumedCountPromise.tryFailure(error)
      }
      override def onComplete(): Unit = {
        super.onComplete()
        log.info(s"Completed - consumed: ${consumedCount.get()}")
        consumedCountPromise.trySuccess(consumedCount.get())
      }
    }
    publisher.subscribe(subscriber)
    consumedCountPromise.future
      .map(count => assert(count == totalRows))
  }

  it should "emit elements in order" in {
    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      SQL(s"select id from $tableName order by id")
        .map(_.int("id"))
        .iterator()
    }

    val expectedElements = (1 to totalRows)
    val actualElements = new ListBuffer[Int]
    val consumedCountPromise: Promise[ListBuffer[Int]] =
      Promise[ListBuffer[Int]]()
    val subscriber: SyncSubscriber[Int] = new SyncSubscriber[Int] {
      override def whenNext(element: Int): Boolean = {
        actualElements += element
        log.info(s"foreach element: $element")
        true
      }
      override def onError(error: Throwable): Unit = {
        super.onError(error)
        log.info(s"Error - ${error}, consumed: ${actualElements.size}")
        consumedCountPromise.tryFailure(error)
      }
      override def onComplete(): Unit = {
        super.onComplete()
        log.info(s"Completed - consumed: ${actualElements.size}")
        consumedCountPromise.trySuccess(actualElements)
      }
    }
    publisher.subscribe(subscriber)
    consumedCountPromise.future
      .map(actualElements => assert(actualElements == expectedElements))
  }

  it should "be subscribed and use the modified DB session" in {
    val passedStreamReadySwitcher: AtomicBoolean = new AtomicBoolean(false)
    val publisher: DatabasePublisher[Int] = DB.readOnlyStream {
      SQL(s"select id from $tableName")
        .map(_.int("id"))
        .iterator()
        .withDBSessionForceAdjuster(session => {
          passedStreamReadySwitcher.set(true)
        })
    }

    val consumedCountPromise: Promise[Int] = Promise[Int]()
    val subscriber: SyncSubscriber[Int] = new SyncSubscriber[Int] {
      private[this] val consumedCount = new AtomicInteger(0)

      override def whenNext(element: Int): Boolean = {
        val consumed = consumedCount.incrementAndGet()
        log.info(s"foreach element: $element, consumed: $consumed")
        true
      }
      override def onError(error: Throwable): Unit = {
        super.onError(error)
        log.info(s"Error - ${error}, consumed: ${consumedCount.get()}")
        consumedCountPromise.tryFailure(error)
      }
      override def onComplete(): Unit = {
        super.onComplete()
        log.info(s"Completed - consumed: ${consumedCount.get()}")
        consumedCountPromise.trySuccess(consumedCount.get())
      }
    }
    publisher.subscribe(subscriber)
    consumedCountPromise.future
      .map(count =>
        assert(count == totalRows && passedStreamReadySwitcher.get())
      )
  }

  // ------------------------------------------
  // AsyncSubscriber
  // ------------------------------------------

  it should "be subscribed by AsyncSubscriber" in {
    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      SQL(s"select id from $tableName").map(_.int("id")).iterator()
    }

    val consumedCountPromise: Promise[Int] = Promise[Int]()
    val executor = Executors.newFixedThreadPool(5)
    val subscriber = new AsyncSubscriber[Int](executor) {
      private[this] val consumedCount = new AtomicInteger(0)

      override def whenNext(element: Int): Boolean = {
        val consumed = consumedCount.incrementAndGet()
        log.info(s"[async] whenNext element: $element, consumed: $consumed")
        true
      }
      override def whenComplete(): Unit = {
        log.info(s"[async] Completed - consumed: ${consumedCount.get()}")
        super.whenComplete()
        consumedCountPromise.trySuccess(consumedCount.get())
        shutdownNow()
      }
      override def whenError(error: Throwable): Unit = {
        log.info(s"[async] Error - ${error}, consumed: ${consumedCount.get()}")
        super.whenError(error)
        consumedCountPromise.tryFailure(error)
        shutdownNow()
      }

      def shutdownNow(): Unit = {
        if (executor.awaitTermination(1, TimeUnit.SECONDS) == false) {
          log.warn("[async] Timed out while waiting all tasks termination")
        }
        executor.shutdownNow()
      }
    }
    publisher.subscribe(subscriber)

    consumedCountPromise.future
      .map(count => assert(count == totalRows))
  }

  it should "be subscribed and cancelled by AsyncSubscriber" in {
    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      SQL(s"select id from $tableName").map(_.int("id")).iterator()
    }

    val expectedCountOfElements = 20
    val consumedCountPromise: Promise[Int] = Promise[Int]()
    val executor = Executors.newFixedThreadPool(5)
    val subscriber = new AsyncSubscriber[Int](executor) {
      val consumedCount = new AtomicInteger(0)

      override def whenNext(element: Int): Boolean = {
        val consumed = consumedCount.incrementAndGet()
        log.info(s"[async] whenNext element: $element, consumed: $consumed")
        val needMore = consumed < expectedCountOfElements
        if (needMore == false) {
          consumedCountPromise.trySuccess(consumedCount.get())
          shutdownNow()
        }
        needMore
      }
      // the following event handlers won't be called.
      // check the source code of AsyncSubscriber
      // https://github.com/reactive-streams/reactive-streams-jvm/blob/223ef95e06d1fc30259c867bdcfe9265e60832a8/examples/src/main/java/org/reactivestreams/example/unicast/AsyncSubscriber.java#L194-L204
      override def whenComplete(): Unit = {
        log.info(s"[async] Completed - consumed: ${consumedCount.get()}")
        super.whenComplete()
        shutdownNow()
      }
      override def whenError(error: Throwable): Unit = {
        log.info(s"[async] Error - ${error}, consumed: ${consumedCount.get()}")
        super.whenError(error)
        consumedCountPromise.tryFailure(error)
        shutdownNow()
      }

      def shutdownNow(): Unit = {
        if (executor.awaitTermination(1, TimeUnit.SECONDS) == false) {
          log.warn("[async] Timed out while waiting all tasks termination")
        }
        executor.shutdownNow()
      }
    }
    publisher.subscribe(subscriber)

    consumedCountPromise.future
      .map(count => assert(count == expectedCountOfElements))
  }

}
