package scalikejdbc.metadata

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TableSpec extends AnyFlatSpec with Matchers {

  behavior of "Table"

  it should "be available" in {
    val name: String = ""
    val catalog: String = ""
    val schema: String = ""
    val description: String = ""
    val columns: List[Column] = Nil
    val foreignKeys: List[ForeignKey] = Nil
    val indices: List[Index] = Nil
    val instance = new Table(
      name,
      catalog,
      schema,
      description,
      columns,
      foreignKeys,
      indices
    )
    instance should not be null
  }

}
