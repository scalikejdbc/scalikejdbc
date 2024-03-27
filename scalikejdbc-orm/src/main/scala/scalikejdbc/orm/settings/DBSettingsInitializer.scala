package scalikejdbc.orm.settings

import scalikejdbc.orm.exception.DBSettingsException

/**
 * ORM settings initializer.
 */
trait DBSettingsInitializer {

  private[this] case class State(var alreadyInitialized: Boolean = false)

  private[this] val state: State = State()

  lazy val dbs = DBsWithEnv(ORMEnv.getOrElse(ORMEnv.Development))

  /**
   * Initializes DB settings.
   */
  def initialize(force: Boolean = false): Unit = {
    state.synchronized {
      if (force || !state.alreadyInitialized) {
        if (dbs.dbNames.nonEmpty) {
          dbs.setupAll()
        } else {
          throw new DBSettingsException(s"""
               | ---------------------------------------------
               |
               |  !!! ScalikeJDBC ORM Configuration Error !!!
               |
               |  DB settings was not found. Add some db settings to src/main/resources/application.conf like this:
               |
               |   development {
               |     db {
               |       default {
               |         driver="org.h2.Driver"
               |         url="jdbc:h2:mem:example"
               |         user="sa"
               |         password="sa"
               |         poolInitialSize=2
               |         poolMaxSize=10
               |       }
               |     }
               |   }
               |
               |
               |  "development" is the default env value. If you don't need env prefix it also works.
               |  You can pass env value from environment variables "export scalikejdbc.env=qa" or via JVM option like "-Dscalikejdbc.env=qa".
               |  Though you're not forced to use these values, recommended env values are "development", "test", "qa" and "production".
               |
               | ---------------------------------------------
               |""".stripMargin)
        }
        state.alreadyInitialized = true
      }
    }
  }

  /**
   * Wipes out all DB settings.
   */
  def destroy(): Unit = dbs.closeAll()

}
