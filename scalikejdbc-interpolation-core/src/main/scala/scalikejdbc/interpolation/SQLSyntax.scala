package scalikejdbc.interpolation

/**
 * Value as a part of SQL syntax.
 *
 * This value won't be treated as a binding parameter but will be appended as a part of SQL.
 */
class SQLSyntax private[scalikejdbc] (val value: String, val parameters: Seq[Any] = Vector())

object SQLSyntax {

  private[scalikejdbc] def apply(value: String, parameters: Seq[Any]) = new SQLSyntax(value, parameters)

  def unapply(syntax: SQLSyntax): Option[(String, Seq[Any])] = Some((syntax.value, syntax.parameters))

}

