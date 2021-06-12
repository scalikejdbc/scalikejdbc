package scalikejdbc.metadata

import java.sql.DatabaseMetaData

/**
 * Index type.
 *
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getIndexInfo-java.lang.String-java.lang.String-java.lang.String-boolean-boolean-]]
 */
sealed trait IndexType {
  val typeValue: Short
}

object IndexType {

  def from(value: Short): IndexType = {
    value match {
      case DatabaseMetaData.tableIndexStatistic => tableIndexStatistic
      case DatabaseMetaData.tableIndexClustered => tableIndexClustered
      case DatabaseMetaData.tableIndexHashed    => tableIndexHashed
      case DatabaseMetaData.tableIndexOther     => tableIndexOther
      case _ =>
        tableIndexOther // if some JDBC driver doesn't support metadata API correctly
    }
  }

  case object tableIndexStatistic extends IndexType {
    val typeValue: Short = DatabaseMetaData.tableIndexStatistic
  }
  case object tableIndexClustered extends IndexType {
    val typeValue: Short = DatabaseMetaData.tableIndexClustered
  }
  case object tableIndexHashed extends IndexType {
    val typeValue: Short = DatabaseMetaData.tableIndexHashed
  }
  case object tableIndexOther extends IndexType {
    val typeValue: Short = DatabaseMetaData.tableIndexOther
  }

}
