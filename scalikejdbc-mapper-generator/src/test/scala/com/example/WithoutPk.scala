package com.example

import scalikejdbc._
import org.joda.time.{ DateTime }

case class WithoutPk(
    aaa: String,
    bbb: Option[Int] = None,
    createdAt: DateTime) {

  def save()(implicit session: DBSession = WithoutPk.autoSession): WithoutPk = WithoutPk.save(this)(session)

  def destroy()(implicit session: DBSession = WithoutPk.autoSession): Unit = WithoutPk.delete(this)(session)

}

object WithoutPk {

  val tableName = "WITHOUT_PK"

  object columnNames {
    val aaa = "AAA"
    val bbb = "BBB"
    val createdAt = "CREATED_AT"
    val all = Seq(aaa, bbb, createdAt)
  }

  val * = {
    import columnNames._
    (rs: WrappedResultSet) => WithoutPk(
      aaa = rs.string(aaa),
      bbb = opt[Int](rs.int(bbb)),
      createdAt = rs.timestamp(createdAt).toDateTime)
  }

  object joinedColumnNames {
    val delimiter = "__ON__"
    def as(name: String) = name + delimiter + tableName
    val aaa = as(columnNames.aaa)
    val bbb = as(columnNames.bbb)
    val createdAt = as(columnNames.createdAt)
    val all = Seq(aaa, bbb, createdAt)
    val inSQL = columnNames.all.map(name => tableName + "." + name + " AS " + as(name)).mkString(", ")
  }

  val joined = {
    import joinedColumnNames._
    (rs: WrappedResultSet) => WithoutPk(
      aaa = rs.string(aaa),
      bbb = opt[Int](rs.int(bbb)),
      createdAt = rs.timestamp(createdAt).toDateTime)
  }

  val autoSession = AutoSession

  def find(aaa: String, bbb: Option[Int], createdAt: DateTime)(implicit session: DBSession = autoSession): Option[WithoutPk] = {
    SQL("""SELECT * FROM WITHOUT_PK WHERE AAA = /*'aaa*/'abc' AND BBB = /*'bbb*/1 AND CREATED_AT = /*'createdAt*/'1958-09-06 12:00:00'""")
      .bindByName('aaa -> aaa, 'bbb -> bbb, 'createdAt -> createdAt).map(*).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[WithoutPk] = {
    SQL("""SELECT * FROM WITHOUT_PK""").map(*).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    SQL("""SELECT COUNT(1) FROM WITHOUT_PK""").map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = autoSession): List[WithoutPk] = {
    SQL("""SELECT * FROM WITHOUT_PK WHERE """ + where)
      .bindByName(params: _*).map(*).list.apply()
  }

  def countBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = autoSession): Long = {
    SQL("""SELECT count(1) FROM WITHOUT_PK WHERE """ + where)
      .bindByName(params: _*).map(rs => rs.long(1)).single.apply().get
  }

  def create(
    aaa: String,
    bbb: Option[Int] = None,
    createdAt: DateTime)(implicit session: DBSession = autoSession): WithoutPk = {
    SQL("""
      INSERT INTO WITHOUT_PK (
        AAA,
        BBB,
        CREATED_AT
      ) VALUES (
        /*'aaa*/'abc',
        /*'bbb*/1,
        /*'createdAt*/'1958-09-06 12:00:00'
      )
      """)
      .bindByName(
        'aaa -> aaa,
        'bbb -> bbb,
        'createdAt -> createdAt
      ).update.apply()
    WithoutPk(
      aaa = aaa,
      bbb = bbb,
      createdAt = createdAt)
  }

  def save(m: WithoutPk)(implicit session: DBSession = autoSession): WithoutPk = {
    SQL("""
      UPDATE 
        WITHOUT_PK
      SET 
        AAA = /*'aaa*/'abc',
        BBB = /*'bbb*/1,
        CREATED_AT = /*'createdAt*/'1958-09-06 12:00:00'
      WHERE 
        AAA = /*'aaa*/'abc' AND BBB = /*'bbb*/1 AND CREATED_AT = /*'createdAt*/'1958-09-06 12:00:00'
      """)
      .bindByName(
        'aaa -> m.aaa,
        'bbb -> m.bbb,
        'createdAt -> m.createdAt
      ).update.apply()
    m
  }

  def delete(m: WithoutPk)(implicit session: DBSession = autoSession): Unit = {
    SQL("""DELETE FROM WITHOUT_PK WHERE AAA = /*'aaa*/'abc' AND BBB = /*'bbb*/1 AND CREATED_AT = /*'createdAt*/'1958-09-06 12:00:00'""")
      .bindByName('aaa -> m.aaa, 'bbb -> m.bbb, 'createdAt -> m.createdAt).update.apply()
  }

}
