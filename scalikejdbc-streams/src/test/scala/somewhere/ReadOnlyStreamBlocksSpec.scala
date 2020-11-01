package somewhere

import scalikejdbc._
import scalikejdbc.streams._

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReadOnlyStreamBlocksSpec extends AnyFlatSpec with Matchers {

  "DB.readOnlyStream" should "create DatabasePublisher" in {
    val publisher: DatabasePublisher[Int] = DB readOnlyStream {
      sql"select id from users".map(r => r.int("id")).iterator()
    }
    publisher shouldBe a[DatabasePublisher[_]]
  }

  "NamedDB.readOnlyStream" should "create DatabasePublisher" in {
    val publisher: DatabasePublisher[Long] = NamedDB("default") readOnlyStream {
      sql"select id from users".map(r => r.long("id")).iterator()
    }
    publisher shouldBe a[DatabasePublisher[_]]
  }

}
