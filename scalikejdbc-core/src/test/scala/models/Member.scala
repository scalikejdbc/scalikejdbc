package models

import scalikejdbc._
import java.time.{ LocalDate, LocalDateTime }

case class Member(
  id: Long,
  name: String,
  description: Option[String] = None,
  birthday: Option[LocalDate] = None,
  createdAt: LocalDateTime
) {

  def save()(implicit session: DBSession = AutoSession): Member =
    Member.save(this)(session)

  def destroy()(implicit session: DBSession = AutoSession): Unit =
    Member.delete(this)(session)

}

object Member extends JavaUtilDateConverterImplicits {

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

    (rs: WrappedResultSet) =>
      Member(
        id = rs.get(id),
        name = rs.get(name),
        description = rs.get(description),
        birthday = rs.get(birthday),
        createdAt = rs.get(createdAt)
      )
  }

  def find(
    id: Long
  )(implicit session: DBSession = AutoSession): Option[Member] = {
    SQL("""SELECT * FROM MEMBER WHERE ID = /*'id*/1""")
      .bindByName("id" -> id)
      .map(*)
      .single
      .apply()
  }

  def findAll()(implicit session: DBSession = AutoSession): List[Member] = {
    SQL("""SELECT * FROM MEMBER""").map(*).list.apply()
  }

  def countAll()(implicit session: DBSession = AutoSession): Long = {
    SQL("""SELECT COUNT(1) FROM MEMBER""")
      .map(_.long(1))
      .single
      .apply()
      .get
  }

  def findBy(where: String, params: (String, Any)*)(implicit
    session: DBSession = AutoSession
  ): List[Member] = {
    SQL("""SELECT * FROM MEMBER WHERE """ + where)
      .bindByName(params: _*)
      .map(*)
      .list
      .apply()
  }

  def countBy(where: String, params: (String, Any)*)(implicit
    session: DBSession = AutoSession
  ): Long = {
    SQL("""SELECT count(1) FROM MEMBER WHERE """ + where)
      .bindByName(params: _*)
      .map(_.long(1))
      .single
      .apply()
      .get
  }

  def create(
    id: Long,
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: LocalDateTime
  )(implicit session: DBSession = AutoSession): Member = {
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
          /*'description*/'abc',
          /*'birthday*/'1958-09-06',
          /*'createdAt*/'1958-09-06 12:00:00'
        )
                  """)
      .bindByName(
        "id" -> id,
        "name" -> name,
        "description" -> description,
        "birthday" -> birthday,
        "createdAt" -> createdAt
      )
      .update
      .apply()

    Member(
      id = id,
      name = name,
      description = description,
      birthday = birthday,
      createdAt = createdAt
    )
  }

  def save(m: Member)(implicit session: DBSession = AutoSession): Member = {
    SQL("""
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
        "id" -> m.id,
        "name" -> m.name,
        "description" -> m.description,
        "birthday" -> m.birthday,
        "createdAt" -> m.createdAt
      )
      .update
      .apply()
    m
  }

  def delete(m: Member)(implicit session: DBSession = AutoSession): Unit = {
    SQL("""DELETE FROM MEMBER WHERE ID = /*'id*/1""")
      .bindByName("id" -> m.id)
      .update
      .apply()
  }

}

case class NamedMember(
  id: Long,
  name: String,
  description: Option[String] = None,
  birthday: Option[LocalDate] = None,
  createdAt: LocalDateTime
) {

  def save(): NamedMember = NamedMember.save(this)

  def destroy(): Unit = NamedMember.delete(this)

}

object NamedMember {

  val tableName = "NAMED_MEMBER"

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
    (rs: WrappedResultSet) =>
      NamedMember(
        id = rs.get(id),
        name = rs.get(name),
        description = rs.get(description),
        birthday = rs.get(birthday),
        createdAt = rs.get(createdAt)
      )
  }

  def find(id: Long)(implicit
    session: DBSession = NamedAutoSession("named")
  ): Option[NamedMember] = {
    SQL("""SELECT * FROM NAMED_MEMBER WHERE ID = /*'id*/1""")
      .bindByName("id" -> id)
      .map(*)
      .single
      .apply()
  }

  def findAll()(implicit
    session: DBSession = NamedAutoSession("named")
  ): List[NamedMember] = {
    SQL("""SELECT * FROM NAMED_MEMBER""").map(*).list.apply()
  }

  def countAll()(implicit
    session: DBSession = NamedAutoSession("named")
  ): Long = {
    SQL("""SELECT COUNT(1) FROM NAMED_MEMBER""")
      .map(_.long(1))
      .single
      .apply()
      .get
  }

  def findBy(where: String, params: (String, Any)*)(implicit
    session: DBSession = NamedAutoSession("named")
  ): List[NamedMember] = {
    SQL("""SELECT * FROM NAMED_MEMBER WHERE """ + where)
      .bindByName(params: _*)
      .map(*)
      .list
      .apply()
  }

  def countBy(where: String, params: (String, Any)*)(implicit
    session: DBSession = NamedAutoSession("named")
  ): Long = {
    SQL("""SELECT count(1) FROM NAMED_MEMBER WHERE """ + where)
      .bindByName(params: _*)
      .map(_.long(1))
      .single
      .apply()
      .get
  }

  def create(
    id: Long,
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: LocalDateTime
  )(implicit session: DBSession = NamedAutoSession("named")): NamedMember = {
    SQL("""
        INSERT INTO NAMED_MEMBER (
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
        "id" -> id,
        "name" -> name,
        "description" -> description,
        "birthday" -> birthday,
        "createdAt" -> createdAt
      )
      .update
      .apply()

    NamedMember(
      id = id,
      name = name,
      description = description,
      birthday = birthday,
      createdAt = createdAt
    )
  }

  def save(
    m: NamedMember
  )(implicit session: DBSession = NamedAutoSession("named")): NamedMember = {
    SQL("""
        UPDATE
          NAMED_MEMBER
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
        "id" -> m.id,
        "name" -> m.name,
        "description" -> m.description,
        "birthday" -> m.birthday,
        "createdAt" -> m.createdAt
      )
      .update
      .apply()
    m
  }

  def delete(
    m: NamedMember
  )(implicit session: DBSession = NamedAutoSession("named")): Unit = {
    SQL("""DELETE FROM NAMED_MEMBER WHERE ID = /*'id*/1""")
      .bindByName("id" -> m.id)
      .update
      .apply()
  }

}

object MemberSQLTemplate {

  import Member._

  def find(): Option[Member] = {
    DB readOnly { implicit session =>
      SQL("""SELECT * FROM MEMBER WHERE ID = /*'id*/123""")
        .map(*)
        .single
        .apply()
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
        .map(_.long(1))
        .single
        .apply()
        .get
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
      SQL("""DELETE FROM MEMBER WHERE ID = /*'id*/123""").update.apply()
    }
  }

}
