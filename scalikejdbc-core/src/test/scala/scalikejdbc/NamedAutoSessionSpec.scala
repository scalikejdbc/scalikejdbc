package scalikejdbc

import org.scalatest._
import java.sql._
import util.control.Exception._

class NamedAutoSessionSpec extends FlatSpec with Matchers {

  behavior of "NamedAutoSession"

  it should "be available" in {
    val name: Any = null
    val instance = NamedAutoSession(name)
    instance should not be null
  }

}
