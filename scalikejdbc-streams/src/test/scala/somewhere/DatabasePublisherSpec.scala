package somewhere

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ ExecutorService, ThreadFactory, ThreadPoolExecutor, TimeUnit }

import org.reactivestreams.example.unicast.{ AsyncSubscriber, SyncSubscriber }
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

  private val streamSize = 20

  override protected def beforeAll(): Unit = {
    initDatabaseSettings()
    initializeFixtures(tableName, streamSize)
  }

  override protected def afterAll(): Unit = {
    dropTable(tableName)
  }

  behavior of "DatabasePublisher"

  it should "be subscribed by SyncSubscriber" in {
    val promise = Promise[Int]()
    val subscriber = new SyncSubscriber[Int] {
      private[this] var previousElement = 0

      override def foreach(element: Int): Boolean = {
        log.debug(s"whenNext element=$element")
        if (element < previousElement) {
          false
        } else {
          previousElement = element
          true
        }
      }

      override def onError(error: Throwable): Unit = {
        super.onError(error)
        promise.tryFailure(error)
      }

      override def onComplete(): Unit = {
        super.onComplete()
        promise.trySuccess(previousElement)
      }
    }

    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      SQL(s"select id from $tableName").map(r => r.int("id")).iterator
    }

    publisher.subscribe(subscriber)

    promise.future.map(result => assert(result == streamSize))
  }

  it should "be subscribed by AsyncSubscriber" in {
    val executor = DatabasePublisherSpec.newExecutor()
    val promise = Promise[Int]()
    val subscriber = new AsyncSubscriber[Int](executor) {
      private[this] var previousElement = 0

      override def whenNext(element: Int): Boolean = {
        log.debug(s"whenNext element=$element")
        if (element < previousElement) {
          false
        } else {
          previousElement = element
          true
        }
      }

      override def whenComplete(): Unit = {
        super.whenComplete()
        promise.trySuccess(previousElement)
      }

      override def whenError(error: Throwable): Unit = {
        super.whenError(error)
        promise.tryFailure(error)
      }
    }

    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      SQL(s"select id from $tableName").map(r => r.int("id")).iterator
    }

    publisher.subscribe(subscriber)

    promise.future.map(result => assert(result == streamSize))
      .andThen {
        case _ =>
          executor.shutdownNow()
          if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Failed to terminate ExecutorService after waiting 30 seconds")
          }
      }
  }
}

object DatabasePublisherSpec {

  private def newExecutor(): ExecutorService = {
    val queue = new java.util.concurrent.LinkedBlockingQueue[Runnable]()
    val tf = new DaemonThreadFactory("AsyncSubscriberSpec-")
    new ThreadPoolExecutor(1, 10, 1000L, TimeUnit.MILLISECONDS, queue, tf)
  }

  private class DaemonThreadFactory(namePrefix: String) extends ThreadFactory {
    private[this] val group = Option(System.getSecurityManager).fold(Thread.currentThread.getThreadGroup)(_.getThreadGroup)
    private[this] val threadNumber = new AtomicInteger(1)

    def newThread(r: Runnable): Thread = {
      val t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement, 0)
      if (!t.isDaemon) t.setDaemon(true)
      if (t.getPriority != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY)
      t
    }
  }
}
