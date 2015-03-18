package scalikejdbc

/**
 * Exception which represents too many rows returned.
 */
case class TooManyRowsException(expected: Int, actual: Int) extends Exception
