package scalikejdbc.metadata

/**
 * Index meta data
 *
 * @param name name
 * @param columnNames target column names
 * @param isUnique unique index
 */
case class Index(name: String, columnNames: List[String], isUnique: Boolean)
