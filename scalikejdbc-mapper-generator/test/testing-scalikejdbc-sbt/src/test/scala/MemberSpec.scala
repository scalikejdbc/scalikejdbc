import com.example.models.Member

import org.scalatest._
import org.scalatest.matchers._

class MemberSpec extends FlatSpec with ShouldMatchers with BeforeAfter {

  behavior of "Member"

  def before = {
    // TODO prepare
  }

  def after = {
  }

  it should "have #find" in {
    val created = Member.create(
      name = "find",
      description = None,
      birthday = None,
      createdAt = DateTime.now)
    ) should not be(null)
    Member.find(created.id).isDefined should be(true)
  }

  it should "have #findAll" in {
    Member.findAll().size should > 0
  }

  it should "have #countAll" in {
    Member.countAll() should > 0
  }

}

