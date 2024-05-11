package basic_test

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc._
import scalikejdbc.orm.CRUDMapper
import util.DBSeeds

class Test006Spec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.add(
    "test006",
    "jdbc:h2:mem:test006;MODE=PostgreSQL",
    "sa",
    "sa"
  )

  override val dbSeedsAutoSession: DBSession = NamedAutoSession("test006")

  addSeedSQL(
    sql"create table summary (id bigserial not null, name varchar(100) not null)"
  )
  runIfFailed(sql"select count(1) from summary")

  // entities
  var (
    _beforeCreate,
    _beforeUpdateBy,
    _beforeDeleteBy,
    _afterCreate,
    _afterDeleteBy,
    _afterUpdateBy
  ) =
    (0, 0, 0, 0, 0, 0)

  case class Summary(id: Long, name: String)
  object Summary extends CRUDMapper[Summary] {
    override val connectionPoolName: Any = "test006"
    override def defaultAlias = createAlias("s")

    beforeCreate((session: DBSession, namedValues: Seq[(SQLSyntax, Any)]) => {
      _beforeCreate += 1
    })
    afterCreate(
      (
        session: DBSession,
        namedValues: Seq[(SQLSyntax, Any)],
        generatedId: Option[Long]
      ) => {
        _afterCreate += 1
      }
    )

    beforeUpdateBy(
      (s: DBSession, where: SQLSyntax, params: Seq[(SQLSyntax, Any)]) => {
        _beforeUpdateBy += 1
      }
    )
    afterUpdateBy(
      (
        s: DBSession,
        where: SQLSyntax,
        params: Seq[(SQLSyntax, Any)],
        count: Int
      ) => {
        _afterUpdateBy += 1
      }
    )

    beforeDeleteBy((s: DBSession, where: SQLSyntax) => {
      _beforeDeleteBy += 1
    })
    afterDeleteBy((s: DBSession, where: SQLSyntax, deletedCount: Int) => {
      _afterDeleteBy += 1
    })

    beforeCreate((session: DBSession, namedValues: Seq[(SQLSyntax, Any)]) => {
      _beforeCreate += 1
    })
    afterCreate(
      (
        session: DBSession,
        namedValues: Seq[(SQLSyntax, Any)],
        generatedId: Option[Long]
      ) => {
        _afterCreate += 1
      }
    )

    beforeUpdateBy(
      (s: DBSession, where: SQLSyntax, params: Seq[(SQLSyntax, Any)]) => {
        _beforeUpdateBy += 1
      }
    )
    afterUpdateBy(
      (
        s: DBSession,
        where: SQLSyntax,
        params: Seq[(SQLSyntax, Any)],
        count: Int
      ) => {
        _afterUpdateBy += 1
      }
    )

    beforeDeleteBy((s: DBSession, where: SQLSyntax) => {
      _beforeDeleteBy += 1
    })
    afterDeleteBy((s: DBSession, where: SQLSyntax, deletedCount: Int) => {
      _afterDeleteBy += 1
    })

    override def extract(rs: WrappedResultSet, rn: ResultName[Summary]) =
      autoConstruct(rs, rn)
  }

  def fixture(implicit session: DBSession): Unit = {}

  describe("The test") {
    it("should work as expected") {
      NamedDB("test006").localTx { implicit session =>
        fixture(session)

        _beforeCreate should equal(0)
        _afterCreate should equal(0)
        _beforeUpdateBy should equal(0)
        _afterUpdateBy should equal(0)
        _beforeDeleteBy should equal(0)
        _afterDeleteBy should equal(0)

        val id = Summary.createWithAttributes("name" -> "Sample")
        Summary.updateById(id).withAttributes("name" -> "Sample2")
        Summary.deleteById(id)

        _beforeCreate should equal(2)
        _afterCreate should equal(2)
        _beforeUpdateBy should equal(2)
        _afterUpdateBy should equal(2)
        _beforeDeleteBy should equal(2)
        _afterDeleteBy should equal(2)
      }
    }
  }
}
