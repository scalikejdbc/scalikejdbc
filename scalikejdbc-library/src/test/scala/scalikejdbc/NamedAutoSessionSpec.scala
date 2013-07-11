package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import java.sql._
import util.control.Exception._

class NamedAutoSessionSpec extends FlatSpec with ShouldMatchers {

  behavior of "NamedAutoSession"

  it should "be available" in {
    val name: Any = null
    val instance = new NamedAutoSession(name)
    instance should not be null
  }

}
