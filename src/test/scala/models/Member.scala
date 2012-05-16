package models

import scalikejdbc._
import org.joda.time.{ LocalDate, DateTime }

case class Member(id: Long,
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime) {

  def save(): Member = Member.save(this)

  def destroy(): Unit = Member.delete(this)

}

object Member {

  val tableName = "MEMBER"

  object columnNames {
    val id = "ID"
    val name = "NAME"
    val description = "DESCRIPTION"
    val birthday = "BIRTHDAY"
    val createdAt = "CREATED_AT"
    val all = Seq(id, name, description, birthday, createdAt)
  }

  val * = {
    import columnNames._
    (rs: WrappedResultSet) => Member(
      id = rs.long(id),
      name = rs.string(name),
      description = Option(rs.string(description)),
      birthday = Option(rs.date(birthday)).map(_.toLocalDate),
      createdAt = rs.timestamp(createdAt).toDateTime)
  }

  def find(id: Long)(implicit session: DBSession = NoDBSession()): Option[Member] = {
    val sql = SQL("""SELECT * FROM MEMBER WHERE ID = /*'id*/1""").bindByName('id -> id).map(*).single
    session match {
      case _: NoDBSession => DB readOnly (implicit session => sql.apply())
      case _ => sql.apply()
    }
  }

  def findAll()(implicit session: DBSession = NoDBSession()): List[Member] = {
    val sql = SQL("""SELECT * FROM MEMBER""").map(*).list
    session match {
      case _: NoDBSession => DB readOnly (implicit session => sql.apply())
      case _ => sql.apply()
    }
  }

  def countAll()(implicit session: DBSession = NoDBSession()): Long = {
    val sql = SQL("""SELECT COUNT(1) FROM MEMBER""").map(rs => rs.long(1)).single
    session match {
      case _: NoDBSession => DB readOnly (implicit session => sql.apply().get)
      case _ => sql.apply().get
    }
  }

  def findBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = NoDBSession()): List[Member] = {
    val sql = SQL("""SELECT * FROM MEMBER WHERE """ + where).bindByName(params: _*).map(*).list
    session match {
      case _: NoDBSession => DB readOnly (implicit session => sql.apply())
      case _ => sql.apply()
    }
  }

  def countBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = NoDBSession()): Long = {
    val sql = SQL("""SELECT count(1) FROM MEMBER WHERE """ + where).bindByName(params: _*).map(rs => rs.long(1)).single
    session match {
      case _: NoDBSession => DB readOnly (implicit session => sql.apply().get)
      case _ => sql.apply().get
    }
  }

  def create(
    id: Long,
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime)(implicit session: DBSession = NoDBSession()): Member = {
    val sql = SQL("""
        INSERT INTO MEMBER (
          ID,
          NAME,
          DESCRIPTION,
          BIRTHDAY,
          CREATED_AT
        ) VALUES (
          /*'id*/123,
          /*'name*/'abc',
          /*'description*/'abc',
          /*'birthday*/'1958-09-06',
          /*'createdAt*/'1958-09-06 12:00:00'
        )
                  """)
      .bindByName(
        'id -> id,
        'name -> name,
        'description -> description,
        'birthday -> birthday,
        'createdAt -> createdAt
      ).update

    session match {
      case _: NoDBSession => DB localTx (implicit session => sql.apply())
      case _ => sql.apply()
    }
    Member(
      id = id,
      name = name,
      description = description,
      birthday = birthday,
      createdAt = createdAt
    )
  }

  def save(m: Member)(implicit session: DBSession = NoDBSession()): Member = {
    val sql = SQL("""
        UPDATE
          MEMBER
        SET
          ID = /*'id*/1,
          NAME = /*'name*/'abc',
          DESCRIPTION = /*'description*/'abc',
          BIRTHDAY = /*'birthday*/'1958-09-06',
          CREATED_AT = /*'createdAt*/'1958-09-06 12:00:00'
        WHERE
          ID = /*'id*/1
                  """)
      .bindByName(
        'id -> m.id,
        'name -> m.name,
        'description -> m.description,
        'birthday -> m.birthday,
        'createdAt -> m.createdAt
      ).update
    session match {
      case _: NoDBSession => DB localTx (implicit session => sql.apply())
      case _ => sql.apply()
    }
    m
  }

  def delete(m: Member)(implicit session: DBSession = NoDBSession()): Unit = {
    val sql = SQL("""DELETE FROM MEMBER WHERE ID = /*'id*/1""")
      .bindByName('id -> m.id).update
    session match {
      case _: NoDBSession => DB localTx (implicit session => sql.apply())
      case _ => sql.apply()
    }
  }

}

object MemberSQLTemplate {

  import Member._

  def find(): Option[Member] = {
    DB readOnly { implicit session =>
      SQL("""SELECT * FROM MEMBER WHERE ID = /*'id*/123""")
        .map(*).single.apply()
    }
  }

  def findAll(): List[Member] = {
    DB readOnly { implicit session =>
      SQL("""SELECT * FROM MEMBER""").map(*).list.apply()
    }
  }

  def countAll(): Long = {
    DB readOnly { implicit session =>
      SQL("""SELECT COUNT(1) FROM MEMBER""")
        .map(rs => rs.long(1)).single.apply().get
    }
  }

  def create(): Member = {
    DB localTx { implicit session =>
      SQL("""
        INSERT INTO MEMBER (
          ID,
          NAME,
          DESCRIPTION,
          BIRTHDAY,
          CREATED_AT
        ) VALUES (
          /*'id*/123,
          /*'name*/'abc',
          /*'description*/'xxx',
          /*'birthday*/'1980-04-06',
          /*'createdAt*/'2012-05-06 12:34:56'
        )
          """).update.apply()
    }
    Member.find(123).get
  }

  def save(): Member = {
    DB localTx { implicit session =>
      SQL("""
        UPDATE
          MEMBER
        SET
          ID = /*'id*/123,
          NAME = /*'name*/'xxx',
          DESCRIPTION = /*'description*/'yyyy',
          BIRTHDAY = /*'birthday*/'1980-12-30',
          CREATED_AT = /*'createdAt*/'2012-05-04 12:23:34'
        WHERE
          ID = /*'id*/123
          """).update.apply()
    }
    Member.find(123).get
  }

  def delete(): Unit = {
    DB localTx { implicit session =>
      SQL("""DELETE FROM MEMBER WHERE ID = /*'id*/123""")
        .update.apply()
    }
  }

}
