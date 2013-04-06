package models

import java.io.File
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import org.specs2.specification._

import scalikejdbc._

class UserSpec extends Specification {

  "User" should {
    "have #create and #findByEmail" in {
      running (FakeApplication(path = new File("scalikejdbc-play-plugin/test/zentasks"))) {
        NamedDB('secondary) localTx {
          implicit session =>
          val user = User.findByEmail("seratch@gmail.com").getOrElse {
            User.create(
              User(
                email = "seratch@gmail.com",
                name = "seratch",
                password = "play20"
              )
            )
          }
          user.name must equalTo("seratch")
        }
      }
    }
  }

}

