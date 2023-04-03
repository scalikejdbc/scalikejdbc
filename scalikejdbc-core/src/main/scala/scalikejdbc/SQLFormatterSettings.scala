package scalikejdbc

/**
 * Settings for SQL formatter
 */
case class SQLFormatterSettings(formatterClassName: Option[String])
  extends LogSupport {

  lazy val formatter: Option[SQLFormatter] = formatterClassName.flatMap {
    className =>
      try {
        val clazz = Class.forName(className)
        Some(
          clazz
            .getDeclaredConstructor()
            .newInstance()
            .asInstanceOf[SQLFormatter]
        )
      } catch {
        case e: Exception =>
          log.warn("Failed to load " + className)
          None
      }
  }

}

object SQLFormatterSettings {

  def apply(): SQLFormatterSettings = SQLFormatterSettings(None)

  def apply(formatterClassName: String): SQLFormatterSettings =
    SQLFormatterSettings(Some(formatterClassName))

}
