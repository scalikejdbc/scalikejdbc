package scalikejdbc.scalatest

import scalikejdbc._
import org.joda.time.DateTime

object ScalaTestMember {

  def count()(implicit session: DBSession = AutoSession): Long = {
    SQL("select count(1) from scalatest_members")
      .map(_.long(1))
      .single
      .apply()
      .get
  }

  def create(id: Long, name: String)(implicit
    session: DBSession = AutoSession
  ): Unit = {
    SQL("insert into scalatest_members values (?, ?, ?)")
      .bind(id, name, DateTime.now)
      .update
      .apply()
  }

  def delete(id: Long)(implicit session: DBSession = AutoSession): Unit = {
    SQL("delete from scalatest_members where id = ?").bind(id).update.apply()
  }

}
