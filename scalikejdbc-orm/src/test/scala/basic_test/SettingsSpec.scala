package basic_test

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc.orm.settings.{ DBSettingsInitializer, ORMEnv }

class SettingsSpec extends AnyFunSpec with Matchers with DBSettingsInitializer {

  describe("hasManyThrough without byDefault") {
    it("should work as expected") {
      val originalValue = System.getProperty(ORMEnv.EnvKey)
      try {
        System.setProperty(ORMEnv.EnvKey, "test")
        initialize()
        initialize(true)
        destroy()
      } finally {
        if (originalValue != null) {
          System.setProperty(ORMEnv.EnvKey, originalValue)
        }
      }
    }
  }
}
