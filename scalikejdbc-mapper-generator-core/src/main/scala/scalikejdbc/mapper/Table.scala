package scalikejdbc.mapper

case class Table(
  name: String,
  allColumns: List[Column],
  autoIncrementColumns: List[Column],
  primaryKeyColumns: List[Column],
  schema: Option[String] = None
)
