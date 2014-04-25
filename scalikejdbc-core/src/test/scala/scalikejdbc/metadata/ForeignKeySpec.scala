package scalikejdbc.metadata

import org.scalatest._

class ForeignKeySpec extends FlatSpec with Matchers {

  behavior of "ForeignKey"

  it should "be available" in {
    val name: String = ""
    val foreignColumnName: String = ""
    val foreignTableName: String = ""
    val instance = new ForeignKey(name, foreignColumnName, foreignTableName)
    instance should not be null
  }

}
