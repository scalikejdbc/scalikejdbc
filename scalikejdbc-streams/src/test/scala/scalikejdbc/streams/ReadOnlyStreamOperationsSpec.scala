package scalikejdbc.streams

import org.scalatest._
import scalikejdbc._

import scala.concurrent.ExecutionContext

class ReadOnlyStreamOperationsSpec extends FlatSpec with Matchers {

  implicit val executor = AsyncExecutor(ExecutionContext.global)

  "DB.readOnlyStream" should "create DatabasePublisher" in {
    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      sql"select id from users".map(r => r.int("id")).iterator
    }
    publisher shouldBe a[DatabasePublisher[_]]
  }

  "NamedDB.readOnlyStream" should "create DatabasePublisher" in {
    val publisher: DatabasePublisher[Long] = NamedDB('default) readOnlyStream {
      sql"select id from users".map(r => r.long("id")).iterator
    }
    publisher shouldBe a[DatabasePublisher[_]]
  }

}
