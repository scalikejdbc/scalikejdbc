package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InformationSchemaSpec
  extends AnyFlatSpec
  with Matchers
  with SQLInterpolation
  with DBSettings {

  behavior of "SQLSyntaxSupport with information_schema"

  case class Role(id: Long, name: String)

  object Roles extends SQLSyntaxSupport[Role] {
    override val tableName = "roles"
    def apply(a: SyntaxProvider[Role])(rs: WrappedResultSet): Role =
      apply(a.resultName)(rs)
    def apply(a: ResultName[Role])(rs: WrappedResultSet): Role =
      new Role(rs.get(a.id), rs.get(a.name))
  }

  it should "work" in {
    val roles: collection.Seq[Role] = DB autoCommit { implicit s =>
      try {
        sql"drop table roles if exists".execute.apply()
      } catch { case e: Exception => }
      try {
        sql"create table roles (id int not null, name varchar(256) not null)".execute
          .apply()
        sql"insert into roles (id, name) values (1, 'Alice')".update.apply()
      } catch { case e: Exception => }

      val r = Roles.syntax("r")
      withSQL { select.from(Roles as r) }.map(Roles(r)).list.apply()
    }
    roles.size should equal(1)
  }

}
