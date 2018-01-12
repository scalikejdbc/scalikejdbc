package app.models

import org.scalatest._
import scalikejdbc.scalatest.AutoRollback

class Issue810 extends fixture.FlatSpec with Matchers with AutoRollback {
  behavior of "AddressStreet"

  it should "be available" in { implicit session =>
    AddressStreet.as.tableAliasName should equal("as_")
    AddressStreet.findAll().toSet should equal(Set())
    AddressStreet.find(-1) should equal(None)
    AddressStreet.countAll() should equal(0)
  }
}
