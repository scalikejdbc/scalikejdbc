package scalikejdbc.metadata

/**
 * Column meta data
 *
 * @param name name
 * @param typeCode type code(int)
 * @param typeName type name
 * @param size size
 * @param isRequired not null
 * @param isPrimaryKey primary key
 * @param isAutoIncrement auto increment
 * @param description comment
 * @param defaultValue default value
 */
case class Column(
  name: String,
  typeCode: Int,
  typeName: String,
  size: Int = 0,
  isRequired: Boolean = false,
  isPrimaryKey: Boolean = false,
  isAutoIncrement: Boolean = false,
  description: String = null,
  defaultValue: String = null
)
