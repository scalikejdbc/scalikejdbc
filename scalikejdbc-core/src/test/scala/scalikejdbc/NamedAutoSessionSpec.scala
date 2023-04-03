package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NamedAutoSessionSpec extends AnyFlatSpec with Matchers {

  behavior of "NamedAutoSession"

  it should "be available" in {
    val name: Any = null
    val instance = new NamedAutoSession(name)
    instance should not be null
  }

}
