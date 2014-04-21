package unit

import scalikejdbc._

trait DBSettings extends LoanPattern {

  try {
    using(ConnectionPool.borrow()) { conn => }
  } catch {
    case e: Exception =>
      Class.forName("org.h2.Driver")
      ConnectionPool.singleton("jdbc:h2:mem:db", "", "")
      ConnectionPool.add('db2, "jdbc:h2:mem:db2", "", "")
  }

}
