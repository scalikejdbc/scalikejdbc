package scalikejdbc.metadata

/**
 * Foreign key meta data
 *
 * @param name column name
 * @param foreignColumnName column name on the foreign table
 * @param foreignTableName foreign table name
 */
case class ForeignKey(
  name: String,
  foreignColumnName: String,
  foreignTableName: String
)
