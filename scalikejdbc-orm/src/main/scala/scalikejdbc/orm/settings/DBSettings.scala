package scalikejdbc.orm.settings

/**
  * ORM settings initializer.
  */
object DBSettings extends DBSettingsInitializer

/**
  * Database settings initializer mixin.
  */
trait DBSettings {

  DBSettings.initialize()

}
