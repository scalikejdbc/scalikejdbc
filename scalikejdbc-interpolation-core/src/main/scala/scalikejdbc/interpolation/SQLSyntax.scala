package scalikejdbc.interpolation

/**
 * Value as a part of SQL syntax.
 *
 * This value won't be treated as a binding parameter but will be appended as a part of SQL.
 */
case class SQLSyntax(value: String, parameters: Seq[Any] = Vector())

