package com.example.placeholder

import scalikejdbc._

case class MemberGroup(
    id: Int,
    name: String) {

  def save()(implicit session: DBSession = MemberGroup.autoSession): MemberGroup = MemberGroup.save(this)(session)

  def destroy()(implicit session: DBSession = MemberGroup.autoSession): Unit = MemberGroup.delete(this)(session)

}

object MemberGroup {

  val tableName = "MEMBER_GROUP"

  object columnNames {
    val id = "ID"
    val name = "NAME"
    val all = Seq(id, name)
  }

  val * = {
    import columnNames._
    (rs: WrappedResultSet) => MemberGroup(
      id = rs.int(id),
      name = rs.string(name))
  }

  object joinedColumnNames {
    val delimiter = "__ON__"
    def as(name: String) = name + delimiter + tableName
    val id = as(columnNames.id)
    val name = as(columnNames.name)
    val all = Seq(id, name)
    val inSQL = columnNames.all.map(name => tableName + "." + name + " AS " + as(name)).mkString(", ")
  }

  val joined = {
    import joinedColumnNames._
    (rs: WrappedResultSet) => MemberGroup(
      id = rs.int(id),
      name = rs.string(name))
  }

  val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[MemberGroup] = {
    SQL("""SELECT * FROM MEMBER_GROUP WHERE ID = ?""")
      .bind(id).map(*).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[MemberGroup] = {
    SQL("""SELECT * FROM MEMBER_GROUP""").map(*).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    SQL("""SELECT COUNT(1) FROM MEMBER_GROUP""").map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: String, params: Any*)(implicit session: DBSession = autoSession): List[MemberGroup] = {
    SQL("""SELECT * FROM MEMBER_GROUP WHERE """ + where)
      .bind(params: _*).map(*).list.apply()
  }

  def countBy(where: String, params: Any*)(implicit session: DBSession = autoSession): Long = {
    SQL("""SELECT count(1) FROM MEMBER_GROUP WHERE """ + where)
      .bind(params: _*).map(rs => rs.long(1)).single.apply().get
  }

  def create(
    name: String)(implicit session: DBSession = autoSession): MemberGroup = {
    val generatedKey = SQL("""
      INSERT INTO MEMBER_GROUP (
        NAME
      ) VALUES (
        ?
      )
      """)
      .bind(
        name
      ).updateAndReturnGeneratedKey.apply()
    MemberGroup(
      id = generatedKey.toInt,
      name = name)
  }

  def save(m: MemberGroup)(implicit session: DBSession = autoSession): MemberGroup = {
    SQL("""
      UPDATE 
        MEMBER_GROUP
      SET 
        ID = ?,
        NAME = ?
      WHERE 
        ID = ?
      """)
      .bind(
        m.id,
        m.name,
        m.id
      ).update.apply()
    m
  }

  def delete(m: MemberGroup)(implicit session: DBSession = autoSession): Unit = {
    SQL("""DELETE FROM MEMBER_GROUP WHERE ID = ?""")
      .bind(m.id).update.apply()
  }

}
