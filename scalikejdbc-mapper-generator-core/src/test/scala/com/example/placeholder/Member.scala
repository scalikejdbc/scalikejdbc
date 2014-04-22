package com.example.placeholder

import scalikejdbc._
import org.joda.time.{ LocalDate, DateTime }

case class Member(
    id: Int,
    name: String,
    memberGroupId: Option[Int] = None,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime) {

  def save()(implicit session: DBSession = Member.autoSession): Member = Member.save(this)(session)

  def destroy()(implicit session: DBSession = Member.autoSession): Unit = Member.destroy(this)(session)

}

object Member extends SQLSyntaxSupport[Member] {

  override val tableName = "MEMBER"

  override val columns = Seq("ID", "NAME", "MEMBER_GROUP_ID", "DESCRIPTION", "BIRTHDAY", "CREATED_AT")

  def apply(m: SyntaxProvider[Member])(rs: WrappedResultSet): Member = apply(m.resultName)(rs)
  def apply(m: ResultName[Member])(rs: WrappedResultSet): Member = new Member(
    id = rs.get(m.id),
    name = rs.get(m.name),
    memberGroupId = rs.get(m.memberGroupId),
    description = rs.get(m.description),
    birthday = rs.get(m.birthday),
    createdAt = rs.get(m.createdAt)
  )

  val m = Member.syntax("m")

  override val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Member] = {
    withSQL {
      select.from(Member as m).where.eq(m.id, id)
    }.map(Member(m.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Member] = {
    withSQL(select.from(Member as m)).map(Member(m.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(Member as m)).map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Member] = {
    withSQL {
      select.from(Member as m).where.append(sqls"${where}")
    }.map(Member(m.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls"count(1)").from(Member as m).where.append(sqls"${where}")
    }.map(_.long(1)).single.apply().get
  }

  def create(
    name: String,
    memberGroupId: Option[Int] = None,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime)(implicit session: DBSession = autoSession): Member = {
    val generatedKey = withSQL {
      insert.into(Member).columns(
        column.name,
        column.memberGroupId,
        column.description,
        column.birthday,
        column.createdAt
      ).values(
          name,
          memberGroupId,
          description,
          birthday,
          createdAt
        )
    }.updateAndReturnGeneratedKey.apply()

    Member(
      id = generatedKey.toInt,
      name = name,
      memberGroupId = memberGroupId,
      description = description,
      birthday = birthday,
      createdAt = createdAt)
  }

  def save(entity: Member)(implicit session: DBSession = autoSession): Member = {
    withSQL {
      update(Member).set(
        column.id -> entity.id,
        column.name -> entity.name,
        column.memberGroupId -> entity.memberGroupId,
        column.description -> entity.description,
        column.birthday -> entity.birthday,
        column.createdAt -> entity.createdAt
      ).where.eq(column.id, entity.id)
    }.update.apply()
    entity
  }

  def destroy(entity: Member)(implicit session: DBSession = autoSession): Unit = {
    withSQL { delete.from(Member).where.eq(column.id, entity.id) }.update.apply()
  }

}
