package scalikejdbc.specs2

import scalikejdbc._
import org.joda.time.DateTime

object Member {

  def count()(implicit session: DBSession = AutoSession): Long = {
    SQL("select count(1) from members").map(_.long(1)).single.apply().get
  }

  def create(id: Long, name: String)(implicit
    session: DBSession = AutoSession
  ): Unit = {
    SQL("insert into members values (?, ?, ?)")
      .bind(id, name, DateTime.now)
      .update
      .apply()
  }

  def delete(id: Long)(implicit session: DBSession = AutoSession): Unit = {
    SQL("delete from members where id = ?").bind(id).update.apply()
  }

}
