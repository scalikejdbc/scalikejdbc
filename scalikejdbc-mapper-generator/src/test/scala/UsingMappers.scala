import org.scalatest._
import org.scalatest.matchers._

import org.joda.time._
import scalikejdbc._
import com.example._

class UsingMappersSpec extends FlatSpec with ShouldMatchers {

  Class.forName("org.h2.Driver")

  val url = "jdbc:h2:file:db/test"
  val username = "sa"
  val password = ""
  ConnectionPool.singleton(url, username, password)

  it should "work fine with Member" in {
    Member.create("Alice" + System.currentTimeMillis, Some("Example"), None, new org.joda.time.DateTime)
    Member.findAll() foreach println
    Member.findBy(Member.columnNames.description + " = /*'description*/'aaa'", 'description -> "Example") foreach println
  }

  it should "work fine with placeholder.Member" in {
    placeholder.Member.create("Alice" + System.currentTimeMillis, Some("Example"), None, new org.joda.time.DateTime)
    placeholder.Member.findAll() foreach println
    placeholder.Member.findBy(Member.columnNames.description + " = ?", "Example") foreach println
  }

  it should "work fine with anorm.Member" in {
    anorm.Member.create("Alice" + System.currentTimeMillis, Some("Example"), None, new org.joda.time.DateTime)
    anorm.Member.findAll() foreach println
    anorm.Member.findBy(Member.columnNames.description + " = {description}", 'description -> "Example") foreach println
  }

  it should "support within tx" in {
    DB readOnly { implicit session =>
      Member.findBy("name = /*'name*/''", 'name -> "Rollback").foreach { member => member.destroy() }
    }
    try {
      DB localTx { implicit session =>
        Member.create("Rollback", Some("Rollback test"), None, new DateTime)
        Member.findBy("name = /*'name*/''", 'name -> "Rollback").size should equal(1)
        throw new RuntimeException
      }
    } catch { case e => }
    Member.findBy("name = /*'name*/''", 'name -> "Rollback").size should equal(0)
  }

  it should "work fine with UnNormalized" in {
    UnNormalized.countAll()
  }

  it should "save UnNormalized value" in {
    val created = UnNormalized.create(
      v01 = 1,
      v02 = 2,
      v03 = 3,
      v04 = 4L,
      v05 = new java.math.BigDecimal("123"),
      v06 = new java.math.BigDecimal("234"),
      v07 = 0.7D,
      v08 = None,
      v10 = "10",
      v16 = Some(true),
      v17 = new LocalDate,
      v18 = new LocalTime,
      v19 = new LocalTime,
      v20 = new DateTime,
      v22 = true,
      v23 = 2.3F,
      v24 = 2.4D,
      createdAt = new DateTime)
    created.copy(v07 = 7.0D).save()
    UnNormalized.find(created.id).get.v07 should equal(7.0D)
  }

  it should "work fine with WithoutPk" in {
    val one = WithoutPk.create(aaa = "aaa", bbb = Some(123), createdAt = new DateTime)
    one.save()
    println(WithoutPk.countAll())
    one.destroy()
  }

}
