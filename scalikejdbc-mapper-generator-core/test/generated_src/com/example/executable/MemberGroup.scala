package com.example.executable

import scalikejdbc._

case class MemberGroup(
  id: Int, 
  name: String, 
  underscore: Option[String] = None) {

  def save()(implicit session: DBSession = MemberGroup.autoSession): MemberGroup = MemberGroup.update(this)(session)

  def destroy()(implicit session: DBSession = MemberGroup.autoSession): Unit = MemberGroup.delete(this)(session)

}
      

object MemberGroup {

  val tableName = "MEMBER_GROUP"

  object columnNames {
    val id = "ID"
    val name = "NAME"
    val underscore = "_UNDERSCORE"
    val all = Seq(id, name, underscore)
    val inSQL = all.mkString(", ")
  }
      
  val * = {
    import columnNames._
    (rs: WrappedResultSet) => MemberGroup(
      id = rs.int(id),
      name = rs.string(name),
      underscore = rs.stringOpt(underscore))
  }
      
  object joinedColumnNames {
    val delimiter = "__ON__"
    def as(name: String) = name + delimiter + tableName
    val id = as(columnNames.id)
    val name = as(columnNames.name)
    val underscore = as(columnNames.underscore)
    val all = Seq(id, name, underscore)
    val inSQL = columnNames.all.map(name => tableName + "." + name + " AS " + as(name)).mkString(", ")
  }
      
  val joined = {
    import joinedColumnNames._
    (rs: WrappedResultSet) => MemberGroup(
      id = rs.int(id),
      name = rs.string(name),
      underscore = rs.stringOpt(underscore))
  }
      
  val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[MemberGroup] = {
    SQL("""select * from MEMBER_GROUP where ID = /*'id*/1""")
      .bindByName('id -> id).map(*).single.apply()
  }
          
  def findAll()(implicit session: DBSession = autoSession): List[MemberGroup] = {
    SQL("""select * from MEMBER_GROUP""").map(*).list.apply()
  }
          
  def countAll()(implicit session: DBSession = autoSession): Long = {
    SQL("""select count(1) from MEMBER_GROUP""").map(rs => rs.long(1)).single.apply().get
  }
          
  def findAllBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = autoSession): List[MemberGroup] = {
    SQL("""select * from MEMBER_GROUP where """ + where)
      .bindByName(params: _*).map(*).list.apply()
  }
      
  def countBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = autoSession): Long = {
    SQL("""select count(1) from MEMBER_GROUP where """ + where)
      .bindByName(params: _*).map(rs => rs.long(1)).single.apply().get
  }
      
  def create(
    name: String,
    underscore: Option[String] = None)(implicit session: DBSession = autoSession): MemberGroup = {
    val generatedKey = SQL("""
      insert into MEMBER_GROUP (
        NAME,
        _UNDERSCORE
      ) values (
        /*'name*/'abc',
        /*'underscore*/'abc'
      )
      """)
      .bindByName(
        'name -> name,
        'underscore -> underscore
      ).updateAndReturnGeneratedKey.apply()

    MemberGroup(
      id = generatedKey.toInt, 
      name = name,
      underscore = underscore)
  }

  def update(m: MemberGroup)(implicit session: DBSession = autoSession): MemberGroup = {
    SQL("""
      update
        MEMBER_GROUP
      set
        ID = /*'id*/1,
        NAME = /*'name*/'abc',
        _UNDERSCORE = /*'underscore*/'abc'
      where
        ID = /*'id*/1
      """)
      .bindByName(
        'id -> m.id,
        'name -> m.name,
        'underscore -> m.underscore
      ).update.apply()
    m
  }
      
  def delete(m: MemberGroup)(implicit session: DBSession = autoSession): Unit = {
    SQL("""delete from MEMBER_GROUP where ID = /*'id*/1""")
      .bindByName('id -> m.id).update.apply()
  }
          
}
