package com.example.querydsl

import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class MemberGroup(
  id: Int, 
  name: String, 
  underscore: Option[String] = None) {

  def save()(implicit session: DBSession = MemberGroup.autoSession): MemberGroup = MemberGroup.save(this)(session)

  def destroy()(implicit session: DBSession = MemberGroup.autoSession): Unit = MemberGroup.destroy(this)(session)

}
      

object MemberGroup extends SQLSyntaxSupport[MemberGroup] {

  override val tableName = "MEMBER_GROUP"

  override val columns = Seq("ID", "NAME", "_UNDERSCORE")

  def apply(mg: ResultName[MemberGroup])(rs: WrappedResultSet): MemberGroup = new MemberGroup(
    id = rs.int(mg.id),
    name = rs.string(mg.name),
    underscore = rs.stringOpt(mg.underscore)
  )
      
  val mg = MemberGroup.syntax("mg")

  val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[MemberGroup] = {
    withSQL { 
      select.from(MemberGroup as mg).where.eq(mg.id, id)
    }.map(MemberGroup(mg.resultName)).single.apply()
  }
          
  def findAll()(implicit session: DBSession = autoSession): List[MemberGroup] = {
    withSQL(select.from(MemberGroup as mg)).map(MemberGroup(mg.resultName)).list.apply()
  }
          
  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(MemberGroup as mg)).map(rs => rs.long(1)).single.apply().get
  }
          
  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[MemberGroup] = {
    withSQL { 
      select.from(MemberGroup as mg).where.append(sqls"${where}")
    }.map(MemberGroup(mg.resultName)).list.apply()
  }
      
  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL { 
      select(sqls"count(1)").from(MemberGroup as mg).where.append(sqls"${where}")
    }.map(_.long(1)).single.apply().get
  }
      
  def create(
    name: String,
    underscore: Option[String] = None)(implicit session: DBSession = autoSession): MemberGroup = {
    val generatedKey = withSQL {
      insert.into(MemberGroup).columns(
        column.name,
        column.underscore
      ).values(
        name,
        underscore
      )
    }.updateAndReturnGeneratedKey.apply()

    MemberGroup(
      id = generatedKey.toInt, 
      name = name,
      underscore = underscore)
  }

  def save(m: MemberGroup)(implicit session: DBSession = autoSession): MemberGroup = {
    withSQL { 
      update(MemberGroup as mg).set(
        mg.id -> m.id,
        mg.name -> m.name,
        mg.underscore -> m.underscore
      ).where.eq(mg.id, m.id)
    }.update.apply()
    m
  }
        
  def destroy(m: MemberGroup)(implicit session: DBSession = autoSession): Unit = {
    withSQL { delete.from(MemberGroup).where.eq(column.id, m.id) }.update.apply()
  }
        
}
