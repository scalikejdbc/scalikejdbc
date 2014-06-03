package scalikejdbc

import org.scalatest._

class NamedAutoSessionSpec extends FlatSpec with Matchers {

  behavior of "NamedAutoSession"

  it should "be available" in {
    val name: Any = null
    val instance = new NamedAutoSession(name)
    instance should not be null
  }

}
