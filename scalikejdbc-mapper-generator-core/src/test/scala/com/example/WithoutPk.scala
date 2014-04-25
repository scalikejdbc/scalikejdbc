package com.example

import scalikejdbc._
import org.joda.time.{ DateTime }

case class WithoutPk(
    aaa: String,
    bbb: Option[Int] = None,
    createdAt: DateTime) {

  def save()(implicit session: DBSession = WithoutPk.autoSession): WithoutPk = WithoutPk.save(this)(session)

  def destroy()(implicit session: DBSession = WithoutPk.autoSession): Unit = WithoutPk.destroy(this)(session)

}

object WithoutPk extends SQLSyntaxSupport[WithoutPk] {

  override val tableName = "WITHOUT_PK"

  override val columns = Seq("AAA", "BBB", "CREATED_AT")

  def apply(wp: SyntaxProvider[WithoutPk])(rs: WrappedResultSet): WithoutPk = apply(wp.resultName)(rs)
  def apply(wp: ResultName[WithoutPk])(rs: WrappedResultSet): WithoutPk = new WithoutPk(
    aaa = rs.get(wp.aaa),
    bbb = rs.get(wp.bbb),
    createdAt = rs.get(wp.createdAt)
  )

  val wp = WithoutPk.syntax("wp")

  override val autoSession = AutoSession

  def find(aaa: String, bbb: Option[Int], createdAt: DateTime)(implicit session: DBSession = autoSession): Option[WithoutPk] = {
    withSQL {
      select.from(WithoutPk as wp).where.eq(wp.aaa, aaa).and.eq(wp.bbb, bbb).and.eq(wp.createdAt, createdAt)
    }.map(WithoutPk(wp.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[WithoutPk] = {
    withSQL(select.from(WithoutPk as wp)).map(WithoutPk(wp.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(WithoutPk as wp)).map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[WithoutPk] = {
    withSQL {
      select.from(WithoutPk as wp).where.append(sqls"${where}")
    }.map(WithoutPk(wp.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls"count(1)").from(WithoutPk as wp).where.append(sqls"${where}")
    }.map(_.long(1)).single.apply().get
  }

  def create(
    aaa: String,
    bbb: Option[Int] = None,
    createdAt: DateTime)(implicit session: DBSession = autoSession): WithoutPk = {
    withSQL {
      insert.into(WithoutPk).columns(
        column.aaa,
        column.bbb,
        column.createdAt
      ).values(
          aaa,
          bbb,
          createdAt
        )
    }.update.apply()

    WithoutPk(
      aaa = aaa,
      bbb = bbb,
      createdAt = createdAt)
  }

  def save(entity: WithoutPk)(implicit session: DBSession = autoSession): WithoutPk = {
    withSQL {
      update(WithoutPk).set(
        column.aaa -> entity.aaa,
        column.bbb -> entity.bbb,
        column.createdAt -> entity.createdAt
      ).where.eq(column.aaa, entity.aaa).and.eq(column.bbb, entity.bbb).and.eq(column.createdAt, entity.createdAt)
    }.update.apply()
    entity
  }

  def destroy(entity: WithoutPk)(implicit session: DBSession = autoSession): Unit = {
    withSQL { delete.from(WithoutPk).where.eq(column.aaa, entity.aaa).and.eq(column.bbb, entity.bbb).and.eq(column.createdAt, entity.createdAt) }.update.apply()
  }

}
