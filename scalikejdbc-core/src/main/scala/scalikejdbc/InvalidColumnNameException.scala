package scalikejdbc

/**
 * Exception which represents invalid key is specified.
 */
case class InvalidColumnNameException(name: String) extends Exception(name)
