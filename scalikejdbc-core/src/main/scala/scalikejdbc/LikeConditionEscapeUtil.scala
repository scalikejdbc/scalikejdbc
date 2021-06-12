package scalikejdbc

/**
 * Utility to escape like condition special characters.
 */
case class LikeConditionEscapeUtil(escapeChar: String) {

  def escape(condition: String): String = {
    condition
      .replace(escapeChar, escapeChar + escapeChar)
      .replace("%", escapeChar + "%")
      .replace("_", escapeChar + "_")
  }

  def beginsWith(value: String): String = escape(value) + "%"

  def endsWith(value: String): String = "%" + escape(value)

  def contains(value: String): String = "%" + escape(value) + "%"

}

object LikeConditionEscapeUtil extends LikeConditionEscapeUtil("\\") {

  val DEFAULT_ESCAPE_CHAR = "\\"
}
