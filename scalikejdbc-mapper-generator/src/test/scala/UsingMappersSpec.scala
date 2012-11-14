import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

import org.joda.time._
import scalikejdbc._
import com.example._

class UsingMappersSpec extends FlatSpec with ShouldMatchers {

  Class.forName("org.h2.Driver")

  val url = "jdbc:h2:file:db/mapper-generator-h2"
  val username = "sa"
  val password = ""
  ConnectionPool.singleton(url, username, password)

  DB autoCommit { implicit session =>
    try {
      SQL("select count(1) from member_group").map(rs => rs).list.apply()
    } catch {
      case e =>
        try {
          SQL("""
            create table member_group (
              id int generated always as identity,
              name varchar(30) not null,
              primary key(id)
            )
            """).execute.apply()
        } catch { case e => }
    }
  }

  DB autoCommit { implicit session =>
    try {
      SQL("select count(1) from member").map(rs => rs).list.apply()
    } catch {
      case e =>
        try {
          SQL("""
            create table member (
              id int generated always as identity,
              name varchar(30) not null,
              member_group_id int,
              description varchar(1000),
              birthday date,
              created_at timestamp not null,
              primary key(id)
            )
            """).execute.apply()
        } catch { case e => }
    }
  }

  DB autoCommit { implicit session =>
    try {
      SQL("select count(1) from un_normalized").map(rs => rs).list.apply()
    } catch {
      case e =>
        try {
          SQL("""
          create table un_normalized (
            id bigint generated always as identity,
            v_01 TINYINT not null,
            v_02 SMALLINT not null,
            v_03 INTEGER not null,
            v_04 BIGINT not null,
            v_05 NUMERIC not null,
            v_06 DECIMAL(10,2) not null,
            v_07 DOUBLE not null,
            v_08 BOOLEAN,
            v_09 CHAR(10),
            v_10 VARCHAR(20) not null,
/*
            v_11 CLOB(30K),
            v_12 LONGVARCHAR,
            v_13 BINARY(10) not null,
            v_14 VARBINARY(10) not null,
            v_15 BLOB(30K),
 */
            v_11 TINYINT,
            v_12 SMALLINT,
            v_13 INTEGER,
            v_14 BIGINT,
            v_15 NUMERIC,
            v_16 BIT(10),
            v_17 DATE not null,
            v_18 TIME not null,
            v_19 TIME(6) not null,
            v_20 TIMESTAMP not null,
            v_21 OTHER,
            v_22 BOOLEAN not null,
            v_23 REAL not null,
            v_24 FLOAT not null,
            created_at timestamp not null,
            primary key(id)
          )
          """).execute.apply()
        } catch { case e => }
    }
  }

  DB autoCommit { implicit session =>
    try {
      SQL("select count(1) from without_pk").map(rs => rs).list.apply()
    } catch {
      case e =>
        try {
          SQL("""
            create table without_pk (
              aaa varchar(30) not null,
              bbb int,
              created_at timestamp not null
            )
            """).execute.apply()
        } catch { case e => }
    }
  }

  it should "work fine with Member" in {
    Member.create("Alice" + System.currentTimeMillis, None, Some("Example"), None, new org.joda.time.DateTime)
    Member.findAll() foreach println
    Member.findAllBy(Member.columnNames.description + " = /*'description*/'aaa'", 'description -> "Example") foreach println
  }

  it should "work fine with placeholder.Member" in {
    placeholder.Member.create("Alice" + System.currentTimeMillis, None, Some("Example"), None, new org.joda.time.DateTime)
    placeholder.Member.findAll() foreach println
    placeholder.Member.findAllBy(Member.columnNames.description + " = ?", "Example") foreach println
  }

  it should "work fine with anorm.Member" in {
    anorm.Member.create("Alice" + System.currentTimeMillis, None, Some("Example"), None, new org.joda.time.DateTime)
    anorm.Member.findAll() foreach println
    anorm.Member.findAllBy(Member.columnNames.description + " = {description}", 'description -> "Example") foreach println
  }

  it should "support within tx" in {
    DB readOnly { implicit session =>
      Member.findAllBy("name = /*'name*/''", 'name -> "Rollback").foreach { member => member.destroy() }
    }
    try {
      DB localTx { implicit session =>
        Member.create("Rollback", None, Some("Rollback test"), None, new DateTime)
        Member.findAllBy("name = /*'name*/''", 'name -> "Rollback").size should equal(1)
        throw new RuntimeException
      }
    } catch { case e => }
    Member.findAllBy("name = /*'name*/''", 'name -> "Rollback").size should equal(0)
  }

  it should "work fine with UnNormalized" in {
    UnNormalized.countAll()
  }

  it should "save UnNormalized value" in {
    val created = UnNormalized.create(
      v01 = 1,
      v02 = 2,
      v03 = 3,
      v04 = 4L,
      v05 = new java.math.BigDecimal("123"),
      v06 = new java.math.BigDecimal("234"),
      v07 = 0.7D,
      v08 = None,
      v10 = "10",
      v16 = Some(true),
      v17 = new LocalDate,
      v18 = new LocalTime,
      v19 = new LocalTime,
      v20 = new DateTime,
      v22 = true,
      v23 = 2.3F,
      v24 = 2.4D,
      createdAt = new DateTime)
    created.copy(v07 = 7.0D).save()
    UnNormalized.find(created.id).get.v07 should equal(7.0D)
  }

  it should "work fine with WithoutPk" in {
    val one = WithoutPk.create(aaa = "aaa", bbb = Some(123), createdAt = new DateTime)
    one.save()
    println(WithoutPk.countAll())
    one.destroy()
  }

  it should "work with join queries" in {
    DB localTx { implicit s =>
      val group = MemberGroup.create("group1")
      Member.findAllBy("name = {name}", 'name -> "WithGroup") match {
        case Nil => Member.create("WithGroup", Some(group.id), Some("xxx"), None, new org.joda.time.DateTime)
        case _ =>
      }
      Member.findAllBy("name = {name}", 'name -> "WithoutGroup") match {
        case Nil => Member.create("WithoutGroup", None, Some("xxx"), None, new org.joda.time.DateTime)
        case _ =>
      }
    }
    val members = DB readOnly { implicit s =>
      val memberWithGroup = (rs: WrappedResultSet) => {
        val member: Member = Member.joined(rs)
        val group: Option[MemberGroup] = rs.longOpt(MemberGroup.joinedColumnNames.id).map(_ => MemberGroup.joined(rs))
        (member, group)
      }
      val columnsInSQL = Seq(Member.joinedColumnNames.inSQL, MemberGroup.joinedColumnNames.inSQL).mkString(", ")
      SQL("select " + columnsInSQL + " from member left outer join member_group on member.member_group_id = member_group.id "
        + "where member.name in ({n1}, {n2}) order by member.id")
        .bindByName('n1 -> "WithGroup", 'n2 -> "WithoutGroup")
        .map(memberWithGroup)
        .list.apply()
    }
    members.size should equal(2)
    members(0)._1.name should equal("WithGroup")
    members(0)._2.isDefined should equal(true)
    members(1)._1.name should equal("WithoutGroup")
    members(1)._2.isDefined should equal(false)
  }

}

