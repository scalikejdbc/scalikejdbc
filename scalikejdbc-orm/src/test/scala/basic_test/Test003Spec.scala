package basic_test

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc._
import scalikejdbc.orm.exception.IllegalAssociationException
import scalikejdbc.orm.{ CRUDMapper, NoIdCRUDMapper }
import util.DBSeeds

class Test003Spec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.add(
    "test003",
    "jdbc:h2:mem:test003;MODE=PostgreSQL",
    "sa",
    "sa"
  )

  override val dbSeedsAutoSession: DBSession = NamedAutoSession("test003")

  addSeedSQL(
    sql"""
create table person (
  id serial not null,
  name varchar(100) not null)
"""
  )
  addSeedSQL(
    sql"""
create table company (
  id serial not null,
  name varchar(100) not null)
"""
  )
  addSeedSQL(
    sql"""
create table employee (
  company_id int not null,
  person_id int not null,
  role varchar(100),
  primary key(company_id, person_id))
"""
  )
  runIfFailed(sql"select count(1) from employee")

  // entities
  case class Person(id: Int, name: String)
  case class Company(id: Int, name: String)
  case class Employee(
    companyId: Int,
    personId: Int,
    role: Option[String],
    company: Option[Company] = None,
    person: Option[Person] = None
  )

  // mappers
  object Person extends CRUDMapper[Person] {
    override val connectionPoolName: Any = "test003"
    override lazy val defaultAlias = createAlias("p")
    override def extract(rs: WrappedResultSet, rn: ResultName[Person]) =
      autoConstruct(rs, rn)
  }

  object Company extends CRUDMapper[Company] {
    override val connectionPoolName: Any = "test003"
    override lazy val defaultAlias = createAlias("c")
    override def extract(rs: WrappedResultSet, rn: ResultName[Company]) =
      autoConstruct(rs, rn)
  }

  object Employee extends NoIdCRUDMapper[Employee] {
    override val connectionPoolName: Any = "test003"
    override lazy val defaultAlias = createAlias("e")
    override def extract(rs: WrappedResultSet, rn: ResultName[Employee]) =
      autoConstruct(rs, rn, "company", "person")

    lazy val personRef = belongsTo[Person](Person, (e, p) => e.copy(person = p))
    lazy val companyRef =
      belongsTo[Company](Company, (e, c) => e.copy(company = c))

    lazy val hasOneRef = hasOne[Company](Company, (e, c) => e.copy(company = c))

    lazy val withAssociations = joins(companyRef, personRef)
  }

  def fixture(implicit session: DBSession): Unit = {
    val p1 = Person.createWithAttributes("name" -> "Alice")
    val p2 = Person.createWithAttributes("name" -> "Bob")
    val p3 = Person.createWithAttributes("name" -> "Chris")
    val c1 = Company.createWithAttributes("name" -> "Google")
    val e1 = Employee.createWithAttributes("companyId" -> c1, "personId" -> p1)
    val e2 =
      Employee.createWithAttributes(
        "companyId" -> c1,
        "personId" -> p2,
        "role" -> "Engineer"
      )
  }

  describe("The test") {
    it("should work as expected") {
      NamedDB("test003").localTx { implicit s =>
        fixture(s)

        {
          // should have finder APIs
          Employee.findAll().size should equal(2)
          Employee.withAssociations.findAll().size should equal(2)
        }

        {
          // should have querying APIs
          val e = Employee.defaultAlias
          val es1 = Employee
            .where(sqls.eq(e.companyId, Company.limit(1).apply().head.id))
            .apply()
          es1.size should equal(2)
        }

        {
          // should detect invalid associations
          intercept[IllegalAssociationException] {
            Employee.joins(Employee.hasOneRef).count()
          }
        }

      }
    }
  }
}
