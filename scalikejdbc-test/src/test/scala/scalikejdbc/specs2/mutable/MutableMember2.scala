package scalikejdbc.specs2.mutable

import scalikejdbc._
import org.joda.time.DateTime

object MutableMember2 {

  def count()(implicit session: DBSession = NamedAutoSession("db2")): Long = {
    SQL("select count(1) from mutable_members2")
      .map(_.long(1))
      .single
      .apply()
      .get
  }

  def create(id: Long, name: String)(implicit
    session: DBSession = NamedAutoSession("db2")
  ): Unit = {
    SQL("insert into mutable_members2 values (?, ?, ?)")
      .bind(id, name, DateTime.now)
      .update
      .apply()
  }

  def delete(
    id: Long
  )(implicit session: DBSession = NamedAutoSession("db2")): Unit = {
    SQL("delete from mutable_members2 where id = ?").bind(id).update.apply()
  }

}
