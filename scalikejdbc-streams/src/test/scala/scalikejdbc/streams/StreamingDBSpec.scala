package scalikejdbc.streams

import org.scalatest._
import scalikejdbc._

import scala.concurrent.ExecutionContext

class StreamingDBSpec extends FlatSpec with Matchers {
  implicit val executor = AsyncExecutor(ExecutionContext.global)

  "DB.stream" should "create DatabasePublisher" in {
    val publisher = DB stream {
      sql"select id from users".map(r => r.int("id")).cursor
    }
    publisher shouldBe a[DatabasePublisher[_]]
  }

  "NamedDB.stream" should "create DatabasePublisher" in {
    val publisher = NamedDB('default) stream {
      sql"select id from users".map(r => r.int("id")).cursor
    }
    publisher shouldBe a[DatabasePublisher[_]]
  }

}
