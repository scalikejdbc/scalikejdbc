package basic_test

import org.joda.time.DateTime
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc._
import scalikejdbc.orm.exception.IllegalAssociationException
import scalikejdbc.orm.timstamps.TimestampsFeature
import scalikejdbc.orm.{ CRUDMapper, NoIdCRUDMapper }
import util.DBSeeds

class Test004Spec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.add(
    "test004",
    "jdbc:h2:mem:test004;MODE=PostgreSQL",
    "sa",
    "sa"
  )

  override val dbSeedsAutoSession: DBSession = NamedAutoSession("test004")

  addSeedSQL(
    sql"""
create table ability_type (
  id serial not null,
  name varchar(100) not null)
"""
  )
  addSeedSQL(
    sql"""
create table ability (
  id serial not null,
  name varchar(100) not null,
  ability_type_id int references ability_type(id),
  created_at timestamp not null,
  updated_at timestamp)
"""
  )
  runIfFailed(sql"select count(1) from ability_type")

  // entities
  case class Ability(
    id: Long,
    name: String,
    abilityTypeId: Option[Long],
    createdAt: DateTime,
    updatedAt: Option[DateTime],
    abilityType: Option[AbilityType] = None
  )

  case class AbilityType(
    id: Long,
    name: String
  )

  // mappers
  object Ability extends CRUDMapper[Ability] with TimestampsFeature[Ability] {
    override val connectionPoolName: Any = "test004"
    override lazy val defaultAlias = createAlias("a")
    lazy val abilityTypeRef =
      belongsTo[AbilityType](AbilityType, (a, at) => a.copy(abilityType = at))
    override def extract(rs: WrappedResultSet, rn: ResultName[Ability]) =
      autoConstruct(rs, rn, "abilityType")
  }

  object AbilityType extends CRUDMapper[AbilityType] {
    override val connectionPoolName: Any = "test004"
    override lazy val defaultAlias = createAlias("at")
    override def extract(rs: WrappedResultSet, rn: ResultName[AbilityType]) =
      autoConstruct(rs, rn)
  }

  def fixture(implicit session: DBSession): Unit = {}

  describe("The test") {
    it("should work as expected") {
      NamedDB("test004").localTx { implicit s =>
        fixture(using s)

        val id = Ability.createWithAttributes("name" -> "SCALA")
        val before: Ability = Ability.findById(id).get
        Thread.sleep(50L)
        Ability.updateById(id).withAttributes("name" -> "Scala")
        val after = Ability.findById(id).get
        after.createdAt should equal(before.createdAt)
        after.updatedAt should not equal (before.updatedAt)
      }
    }
  }
}
