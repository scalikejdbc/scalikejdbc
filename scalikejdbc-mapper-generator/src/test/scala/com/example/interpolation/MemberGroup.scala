package com.example.interpolation

/*
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class MemberGroup(
    id: Int,
    name: String) {

  def save()(implicit session: DBSession = MemberGroup.autoSession): MemberGroup = MemberGroup.update(this)(session)

  def destroy()(implicit session: DBSession = MemberGroup.autoSession): Unit = MemberGroup.delete(this)(session)

}

object MemberGroup {

  val tableName = "MEMBER_GROUP"

  object columnNames {
    val id = "ID"
    val name = "NAME"
    val all = Seq(id, name)
    val inSQL = all.mkString(", ")
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
    sql"""SELECT ${SQLSyntax(columnNames.inSQL)} FROM MEMBER_GROUP WHERE ID = ${id}""".map(*).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[MemberGroup] = {
    sql"""SELECT ${SQLSyntax(columnNames.inSQL)} FROM MEMBER_GROUP""".map(*).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    sql"""SELECT COUNT(1) FROM MEMBER_GROUP""".map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = autoSession): List[MemberGroup] = {
    SQL("""SELECT * FROM MEMBER_GROUP WHERE """ + where)
      .bindByName(params: _*).map(*).list.apply()
  }

  def countBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = autoSession): Long = {
    SQL("""SELECT count(1) FROM MEMBER_GROUP WHERE """ + where)
      .bindByName(params: _*).map(rs => rs.long(1)).single.apply().get
  }

  def create(
    name: String)(implicit session: DBSession = autoSession): MemberGroup = {
    val generatedKey = sql"""
      INSERT INTO MEMBER_GROUP (
        NAME
      ) VALUES (
        ${name}
      )
      """.updateAndReturnGeneratedKey.apply()

    MemberGroup(
      id = generatedKey.toInt,
      name = name)
  }

  def update(m: MemberGroup)(implicit session: DBSession = autoSession): MemberGroup = {
    sql"""
      UPDATE
        MEMBER_GROUP
      SET
        ID = ${m.id},
        NAME = ${m.name}
      WHERE
        ID = ${m.id}
      """.update.apply()
    m
  }

  def delete(m: MemberGroup)(implicit session: DBSession = autoSession): Unit = {
    sql"""DELETE FROM MEMBER_GROUP WHERE ID = ${m.id}""".update.apply()
  }

}
*/
