package app.models

import java.time._
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Random
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

class ProgrammersTest
  extends FixtureAnyFlatSpec
  with Matchers
  with AutoRollback {
  private[this] val p = Programmer.syntax("p")

  behavior of "Programmer"

  app.Initializer.run()

  private[this] def driverClassName(): String = {
    val props = new java.util.Properties()
    val inputStream = new java.io.FileInputStream("test.properties")
    try {
      props.load(inputStream)
    } finally {
      inputStream.close()
    }
    props.get("jdbc.driver").toString
  }

  it should "be available" in { implicit session =>
    if (driverClassName() == "org.h2.Driver") {
      pending
    }
    Programmers.findAll().toSet should equal(Set.empty[Programmers])
    Programmers.countAll() should equal(0)

    val programmers = Seq(Some("aaa"), Some("bbb"), None).map(name =>
      Programmers.create(
        name = name,
        t1 = ZonedDateTime.of(2014, 12, 31, 20, 0, 0, 0, ZoneId.systemDefault)
      )
    )
    programmers.foreach { programmer =>
      Programmers.find(programmer.id) should equal(Some(programmer))
    }
    val invalidId = Int.MinValue
    Programmers.find(invalidId) should equal(None)
    Programmers.findAll().toSet should equal(programmers.toSet)
    Programmers.countAll() should equal(programmers.size)
    Programmers.findAllBy(sqls"${p.name} is not null").toSet should equal(
      programmers.filter(_.name.isDefined).toSet
    )
    Programmers.countBy(sqls"${p.name} is null") should equal(
      programmers.count(_.name.isEmpty)
    )

    val destroyProgrammer = Random.shuffle(programmers).head

    Programmers.destroy(destroyProgrammer)
    Programmers.findAll().toSet should equal(
      programmers.filter(_ != destroyProgrammer).toSet
    )
    Programmers.countAll() should equal(programmers.size - 1)
    Programmers.find(destroyProgrammer.id) should equal(None)

    programmers.foreach(Programmers.destroy(_))
    Programmers.findAll().toSet should equal(Set.empty[Programmers])
    Programmers.countAll() should equal(0)

    val res = Programmers.batchInsert(programmers)
    res.size should equal(programmers.size)
  }

}
