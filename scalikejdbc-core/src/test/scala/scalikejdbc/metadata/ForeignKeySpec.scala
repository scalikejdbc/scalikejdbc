package scalikejdbc.metadata

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ForeignKeySpec extends AnyFlatSpec with Matchers {

  behavior of "ForeignKey"

  it should "be available" in {
    val name: String = ""
    val foreignColumnName: String = ""
    val foreignTableName: String = ""
    val instance = new ForeignKey(name, foreignColumnName, foreignTableName)
    instance should not be null
  }

}
