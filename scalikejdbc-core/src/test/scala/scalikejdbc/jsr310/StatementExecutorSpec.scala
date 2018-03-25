package scalikejdbc
package jsr310

import scalikejdbc.interpolation.Implicits._
import org.scalatest._
import java.time._
import java.time.temporal.ChronoUnit

class StatementExecutorSpec extends FunSpec with Matchers {

  Class.forName("org.h2.Driver")
  ConnectionPool.add('jsr310, "jdbc:h2:mem:jsr310;MODE=PostgreSQL", "", "")

  describe("StatementExecutor") {
    it("should work with JSR-310 APIs") {
      implicit val session = NamedAutoSession('jsr310)

      sql"create table accounts (id bigserial not null, birthday date not null, alert_time time(3) not null, local_created_at timestamp not null, created_at timestamp not null, updated_at timestamp not null )"
        .execute.apply()

      val birthday = LocalDate.now
      val alertTime = LocalTime.now.truncatedTo(ChronoUnit.MILLIS)
      val localCreatedAt = LocalDateTime.now.truncatedTo(ChronoUnit.MILLIS)
      val createdAt = ZonedDateTime.now.truncatedTo(ChronoUnit.MILLIS)
      val updatedAt = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      val query = sql"insert into accounts (birthday, alert_time, local_created_at, created_at, updated_at) values (${birthday}, ${alertTime}, ${localCreatedAt}, ${createdAt}, ${updatedAt})"
      query.execute.apply()

      val account = sql"select birthday, alert_time, local_created_at, created_at, updated_at from accounts limit 1".map { rs =>
        (rs.get[LocalDate]("birthday"), rs.get[LocalTime]("alert_time"), rs.get[LocalDateTime]("local_created_at"), rs.get[ZonedDateTime]("created_at"), rs.get[Instant]("updated_at"))
      }.headOption.apply()

      account.isDefined should equal(true)
      account.get._1 should equal(birthday)
      account.get._2 should equal(alertTime)
      account.get._3 should equal(localCreatedAt)
      account.get._4 should equal(createdAt)
      account.get._5 should equal(updatedAt)
    }
  }

}
