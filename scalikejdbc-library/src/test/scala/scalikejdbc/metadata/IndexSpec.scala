package scalikejdbc.metadata

import org.scalatest._

class IndexSpec extends FlatSpec with Matchers {

  behavior of "Index"

  it should "be available" in {
    val name: String = ""
    val columnNames: List[String] = Nil
    val isUnique: Boolean = false
    val instance = new Index(name, columnNames, isUnique)
    instance should not be null
  }

}
