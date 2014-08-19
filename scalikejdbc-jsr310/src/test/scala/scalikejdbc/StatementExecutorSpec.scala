package scalikejdbc

import org.scalatest._

class StatementExecutorSpec extends FunSpec with Matchers {

  // java.lang.SecurityException: Prohibited package name: java.time on Java 7
  if (sys.props("java.version").startsWith("1.8")) {

    import scalikejdbc.jsr310._
    import java.time._

    Class.forName("org.h2.Driver")
    ConnectionPool.add('jsr310, "jdbc:h2:mem:jsr310;MODE=PostgreSQL", "", "")

    describe("StatementExecutor") {
      it("should work with JSR-310 APIs") {
        implicit val session = NamedAutoSession('jsr310)

        sql"create table accounts (id bigserial not null, birthday date not null, alert_time time not null, local_created_at timestamp not null, created_at timestamp not null )"
          .execute.apply()

        val birthday = LocalDate.now
        val alertTime = LocalTime.now
        val localCreatedAt = LocalDateTime.now
        val createdAt = ZonedDateTime.now
        val query = sql"insert into accounts (birthday, alert_time, local_created_at, created_at) values (${birthday}, ${alertTime}, ${localCreatedAt}, ${createdAt})"
        query.execute.apply()

        val account = sql"select birthday, alert_time, local_created_at, created_at from accounts limit 1".map { rs =>
          (rs.get[LocalDate]("birthday"), rs.get[LocalTime]("alert_time"), rs.get[LocalDateTime]("local_created_at"), rs.get[ZonedDateTime]("created_at"))
        }.headOption.apply()

        account.isDefined should equal(true)
        account.get._1 should equal(birthday)
        account.get._2 should equal(alertTime)
        account.get._3 should equal(localCreatedAt)
        account.get._4 should equal(createdAt)
      }
    }

  } else {
    describe("StatementExecutor") {
      it("should work with Java 7") {
        scalikejdbc.StatementExecutor
        // NOOP
      }
    }
  }

}
