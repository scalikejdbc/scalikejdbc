package com.example.placeholder

import scalikejdbc._
import org.joda.time.{LocalDate, DateTime}

case class Member(
  id: Int, 
  name: String, 
  memberGroupId: Option[Int] = None, 
  description: Option[String] = None, 
  birthday: Option[LocalDate] = None, 
  createdAt: DateTime) {

  def save()(implicit session: DBSession = Member.autoSession): Member = Member.update(this)(session)

  def destroy()(implicit session: DBSession = Member.autoSession): Unit = Member.delete(this)(session)

}
      

object Member {

  val tableName = "MEMBER"

  object columnNames {
    val id = "ID"
    val name = "NAME"
    val memberGroupId = "MEMBER_GROUP_ID"
    val description = "DESCRIPTION"
    val birthday = "BIRTHDAY"
    val createdAt = "CREATED_AT"
    val all = Seq(id, name, memberGroupId, description, birthday, createdAt)
    val inSQL = all.mkString(", ")
  }
      
  val * = {
    import columnNames._
    (rs: WrappedResultSet) => Member(
      id = rs.int(id),
      name = rs.string(name),
      memberGroupId = rs.intOpt(memberGroupId),
      description = rs.stringOpt(description),
      birthday = rs.dateOpt(birthday).map(_.toLocalDate),
      createdAt = rs.timestamp(createdAt).toDateTime)
  }
      
  object joinedColumnNames {
    val delimiter = "__ON__"
    def as(name: String) = name + delimiter + tableName
    val id = as(columnNames.id)
    val name = as(columnNames.name)
    val memberGroupId = as(columnNames.memberGroupId)
    val description = as(columnNames.description)
    val birthday = as(columnNames.birthday)
    val createdAt = as(columnNames.createdAt)
    val all = Seq(id, name, memberGroupId, description, birthday, createdAt)
    val inSQL = columnNames.all.map(name => tableName + "." + name + " AS " + as(name)).mkString(", ")
  }
      
  val joined = {
    import joinedColumnNames._
    (rs: WrappedResultSet) => Member(
      id = rs.int(id),
      name = rs.string(name),
      memberGroupId = rs.intOpt(memberGroupId),
      description = rs.stringOpt(description),
      birthday = rs.dateOpt(birthday).map(_.toLocalDate),
      createdAt = rs.timestamp(createdAt).toDateTime)
  }
      
  val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Member] = {
    SQL("""select * from MEMBER where ID = ?""")
      .bind(id).map(*).single.apply()
  }
          
  def findAll()(implicit session: DBSession = autoSession): List[Member] = {
    SQL("""select * from MEMBER""").map(*).list.apply()
  }
          
  def countAll()(implicit session: DBSession = autoSession): Long = {
    SQL("""select count(1) from MEMBER""").map(rs => rs.long(1)).single.apply().get
  }
          
  def findAllBy(where: String, params: Any*)(implicit session: DBSession = autoSession): List[Member] = {
    SQL("""select * from MEMBER where """ + where)
      .bind(params: _*).map(*).list.apply()
  }
      
  def countBy(where: String, params: Any*)(implicit session: DBSession = autoSession): Long = {
    SQL("""select count(1) from MEMBER where """ + where)
      .bind(params: _*).map(rs => rs.long(1)).single.apply().get
  }
      
  def create(
    name: String,
    memberGroupId: Option[Int] = None,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime)(implicit session: DBSession = autoSession): Member = {
    val generatedKey = SQL("""
      insert into MEMBER (
        NAME,
        MEMBER_GROUP_ID,
        DESCRIPTION,
        BIRTHDAY,
        CREATED_AT
      ) values (
        ?,
        ?,
        ?,
        ?,
        ?
      )
      """)
      .bind(
        name,
        memberGroupId,
        description,
        birthday,
        createdAt
      ).updateAndReturnGeneratedKey.apply()

    Member(
      id = generatedKey.toInt, 
      name = name,
      memberGroupId = memberGroupId,
      description = description,
      birthday = birthday,
      createdAt = createdAt)
  }

  def update(m: Member)(implicit session: DBSession = autoSession): Member = {
    SQL("""
      update
        MEMBER
      set
        ID = ?,
        NAME = ?,
        MEMBER_GROUP_ID = ?,
        DESCRIPTION = ?,
        BIRTHDAY = ?,
        CREATED_AT = ?
      where
        ID = ?
      """)
      .bind(
        m.id,
        m.name,
        m.memberGroupId,
        m.description,
        m.birthday,
        m.createdAt, 
        m.id
      ).update.apply()
    m
  }
      
  def delete(m: Member)(implicit session: DBSession = autoSession): Unit = {
    SQL("""delete from MEMBER where ID = ?""")
      .bind(m.id).update.apply()
  }
          
}
