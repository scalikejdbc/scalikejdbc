package scalikejdbc.metadata

import org.scalatest._

class TableSpec extends FlatSpec with Matchers {

  behavior of "Table"

  it should "be available" in {
    val name: String = ""
    val schema: String = ""
    val description: String = ""
    val columns: List[Column] = Nil
    val foreignKeys: List[ForeignKey] = Nil
    val indices: List[Index] = Nil
    val instance = new Table(name, schema, description, columns, foreignKeys, indices)
    instance should not be null
  }

}
