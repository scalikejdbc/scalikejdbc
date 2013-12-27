package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class EntityEqualitySpec extends FlatSpec with ShouldMatchers {

  behavior of "EntityEquality"

  case class Hoge(id: Long, name: String)

  class Foo(id: Long, name: String) extends EntityEquality {
    override val entityIdentity = (id, name)
  }
  class Bar(id: Long, name: String) extends EntityEquality {
    override val entityIdentity = id
  }

  it should "be available with case classes" in {
    val h1 = new Hoge(1, "Alice")
    val h2 = new Hoge(1, "Alice")
    val h3 = new Hoge(1, "Bob")

    (h1 == h2) should be(true)
    (h1 == h3) should be(false)
    (h2 == h3) should be(false)
  }

  it should "be available with Foo example" in {
    val f1a = new Foo(1, "Alice")
    val f1a_ = new Foo(1, "Alice")
    val f1b = new Foo(1, "Bob")
    val f2a = new Foo(2, "Alice")
    val n: Foo = null

    (f1a == f1a_) should be(true)
    (f1a == f1b) should be(false)
    (f1a == f2a) should be(false)
    (f1b == f2a) should be(false)
    (f1b == n) should be(false)

    (f1a equals f1a_) should be(true)
    (f1a equals f1b) should be(false)
    (f1a equals f2a) should be(false)
    (f1b equals f2a) should be(false)
    (f1b equals n) should be(false)
  }

  it should "be available with Bar example" in {
    val b1a = new Bar(1, "Alice")
    val b1a_ = new Bar(1, "Alice")
    val b1b = new Bar(1, "Bob")
    val b2a = new Bar(2, "Alice")
    val n: Bar = null

    (b1a == b1a_) should be(true)
    (b1a == b1b) should be(true)
    (b1a == b2a) should be(false)
    (b1b == b2a) should be(false)
    (b1b == n) should be(false)

    (b1a equals b1a_) should be(true)
    (b1a equals b1b) should be(true)
    (b1a equals b2a) should be(false)
    (b1b equals b2a) should be(false)
    (b1b equals n) should be(false)

    val f1a = new Foo(1, "Alice")
    (b1a == f1a) should be(false)
  }

}
