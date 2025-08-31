package app.models

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback

class Issue810 extends FixtureAnyFlatSpec with Matchers with AutoRollback {
  behavior of "AddressStreet"

  app.Initializer.run()

  it should "be available" in { implicit session =>
    AddressStreet.as.tableAliasName should equal("as_")
    AddressStreet.findAll().toSet should equal(Set())
    AddressStreet.find(-1) should equal(None)
    AddressStreet.countAll() should equal(0)
  }
}
