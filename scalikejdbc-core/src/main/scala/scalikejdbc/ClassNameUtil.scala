package scalikejdbc

object ClassNameUtil {
  /**
   * Returns clazz.getCanonicalName, or clazz.getName if clazz.getCanonicalName throws Error.
   * @param clazz class
   */
  def getClassName(clazz: Class[_]): String = {
    val canonicalName: Option[String] = try {
      Option(clazz.getCanonicalName)
    } catch {
      case e: InternalError if e.getMessage == "Malformed class name" => None
    }
    canonicalName match {
      case Some(className) => className
      case _ => clazz.getName
    }
  }
}
