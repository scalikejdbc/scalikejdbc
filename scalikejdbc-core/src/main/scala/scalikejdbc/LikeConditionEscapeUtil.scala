package scalikejdbc

/**
 * Utility to escape like condition special characters.
 */
case class LikeConditionEscapeUtil(escapeChar: String) {

  def escape(condition: String): String = {
    condition
      .replaceAllLiterally(escapeChar, escapeChar + escapeChar)
      .replaceAllLiterally("%", escapeChar + "%")
      .replaceAllLiterally("_", escapeChar + "_")
  }

  def beginsWith(value: String): String = escape(value) + "%"

  def endsWith(value: String): String = "%" + escape(value)

  def contains(value: String): String = "%" + escape(value) + "%"

}

object LikeConditionEscapeUtil
  extends LikeConditionEscapeUtil("\\") {

  val DEFAULT_ESCAPE_CHAR = "\\"
}
