package scalikejdbc.mapper

import java.sql.JDBCType

case class Column(
  name: String,
  dataType: JDBCType,
  isNotNull: Boolean,
  isAutoIncrement: Boolean
)
