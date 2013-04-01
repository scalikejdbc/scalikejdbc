package scalikejdbc.play

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import org.specs2.specification.BeforeAfterExample
import play.api.Play.current
import scala.collection.JavaConverters._

class FixtureSupportSpec extends Specification with BeforeAfterExample {

  def before = {
  }

  def after = {
  }

  val fixtureSupport = new FixtureSupport {}

  def fakeApp = FakeApplication(
    additionalConfiguration = Map(
      "db.default.fixtures.test" -> List("users.sql", "project.sql").asJava,
      "db.secondary.fixtures.test" -> "a.sql",
      "db.default.driver" -> "org.h2.Driver",
      "db.default.url" -> "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1",
      "db.default.user" -> "sa",
      "db.default.password" -> "sa",
      "db.secondary.driver" -> "org.h2.Driver",
      "db.secondary.url" -> "jdbc:h2:mem:secondary;DB_CLOSE_DELAY=-1",
      "db.secondary.user" -> "l",
      "db.secondary.password" -> "g"
    ),
    additionalPlugins = Seq("scalikejdbc.PlayPlugin", "scalikejdbc.PlayFixturePlugin")
  )

  "FixtureSupport" should {

    "has #fixtures" in {
      running(fakeApp) {
        fixtureSupport.fixtures must have size 2
      }
    }

  }

}

