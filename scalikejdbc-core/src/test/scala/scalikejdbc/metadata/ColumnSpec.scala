package scalikejdbc.metadata

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ColumnSpec extends AnyFlatSpec with Matchers {

  behavior of "Column"

  it should "be available" in {
    val name: String = ""
    val typeCode: Int = 0
    val typeName: String = ""
    val size: Int = 0
    val isRequired: Boolean = false
    val isPrimaryKey: Boolean = false
    val isAutoIncrement: Boolean = false
    val description: String = ""
    val defaultValue: String = ""
    val instance = new Column(
      name,
      typeCode,
      typeName,
      size,
      isRequired,
      isPrimaryKey,
      isAutoIncrement,
      description,
      defaultValue
    )
    instance should not be null
  }

}
