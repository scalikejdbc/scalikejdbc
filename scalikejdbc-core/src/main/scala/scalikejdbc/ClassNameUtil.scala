package scalikejdbc

private[scalikejdbc] object ClassNameUtil {

  /**
   * Returns the canonical name of a given class. If getCanonicalName doesn't return expected value, this method returns the value came from getName instead.
   * @param clazz a given class object
   */
  def getClassName(clazz: Class[?]): String = {
    val canonicalName: Option[String] =
      try {
        Option(clazz.getCanonicalName)
      } catch {
        case e: InternalError if e.getMessage == "Malformed class name" => None
      }
    canonicalName.getOrElse(clazz.getName)
  }
}
