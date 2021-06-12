package scalikejdbc.specs2.mutable

import scalikejdbc._
import org.joda.time.DateTime

object MutableMember {

  def count()(implicit session: DBSession = AutoSession): Long = {
    SQL("select count(1) from mutable_members")
      .map(_.long(1))
      .single
      .apply()
      .get
  }

  def create(id: Long, name: String)(implicit
    session: DBSession = AutoSession
  ): Unit = {
    SQL("insert into mutable_members values (?, ?, ?)")
      .bind(id, name, DateTime.now)
      .update
      .apply()
  }

  def delete(id: Long)(implicit session: DBSession = AutoSession): Unit = {
    SQL("delete from mutable_members where id = ?").bind(id).update.apply()
  }

}
