package scalikejdbc.metadata

/**
 * Index meta data.
 *
 * @param name name index name; null when TYPE is tableIndexStatistic
 * @param columnNames target column names (collection of COLUMN_NAME without null values: column name; null when TYPE is tableIndexStatistic)
 * @param isUnique unique index (opposite of NON_UNIQUE: Can index values be non-unique. false when TYPE is tableIndexStatistic)
 * @param qualifier index catalog (may be null); null when TYPE is tableIndexStatistic
 * @param indexType index type: tableIndexStatistic, tableIndexClustered, tableIndexHashed, tableIndexOther
 * @param ordinalPosition column sequence number within index; zero when TYPE is tableIndexStatistic
 * @param ascOrDesc column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported; null when TYPE is tableIndexStatistic
 * @param cardinality When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique values in the index.
 * @param pages When TYPE is tableIndexStatistic then this is the number of pages used for the table, otherwise it is the number of pages used for the current index.
 * @param filterCondition Filter condition, if any. (may be null)
 */
case class Index(
  name: String,
  columnNames: List[String],
  isUnique: Boolean,
  qualifier: Option[String],
  indexType: IndexType,
  ordinalPosition: Option[Short],
  ascOrDesc: Option[String],
  cardinality: Option[Long],
  pages: Option[Long],
  filterCondition: Option[String]
)
