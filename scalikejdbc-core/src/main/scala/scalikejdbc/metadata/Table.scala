package scalikejdbc.metadata

/**
 * Table meta data
 *
 * @param name name
 * @param schema schema
 * @param description description
 * @param columns columns
 * @param foreignKeys foreign keys
 * @param indices indices
 */
case class Table(
  name: String,
  schema: String = null,
  description: String = null,
  columns: List[Column] = Nil,
  foreignKeys: List[ForeignKey] = Nil,
  indices: List[Index] = Nil
) {

  // could not use case class field for binary compatibility
  def catalog: String = null

  /**
   * Returns name with schema(if exists)
   *
   * @return name with schema
   */
  def nameWithSchema: String = if (schema == null) name else schema + "." + name

  /**
   * Returns describe style
   *
   * @return describe style string value
   */
  def toDescribeStyleString: String = {
    if (columns.isEmpty) {
      return "table " + name + " does not have any columns"
    }

    def withoutCRLF(str: String): String = {
      if (str == null) null
      else str.replaceAll("\r", "").replaceAll("\n", "")
    }
    def length(str: String): Int = {
      if (str == null) 0
      else
        withoutCRLF(str)
          .map(c => if (c.toString.getBytes.length > 1) 2 else 1)
          .sum
    }
    def take(str: String, maxLength: Int): String = {
      if (str == null) null
      else
        withoutCRLF(str)
          .foldLeft(("", 0)) { case ((str, len), c) =>
            if (len >= maxLength) (str, len)
            else if (len + length(c.toString) > maxLength) (str, maxLength)
            else (str + c, len + length(c.toString))
          }
          ._1
    }

    val maxColumnNameLength = {
      val maxLength =
        columns.map(c => c.name.length).sortWith { case (a, b) => a > b }.head
      if (maxLength < 5) 5 else maxLength
    }
    val maxTypeNameLength = {
      val maxLength = columns
        .map { c =>
          c.typeName.length + (if (c.size > 0 && c.size < 10) 3
                               else if (c.size >= 1000000000) 12
                               else if (c.size >= 100000000) 11
                               else if (c.size >= 10000000) 10
                               else if (c.size >= 1000000) 9
                               else if (c.size >= 100000) 8
                               else if (c.size >= 10000) 7
                               else if (c.size >= 1000) 6
                               else if (c.size >= 100) 5
                               else if (c.size >= 10) 4
                               else 0)
        }
        .sortWith { case (a, b) => a > b }
        .head
      if (maxLength < 4) 4 else maxLength
    }
    val maxDefaultValueLength = {
      val maxLength = columns
        .map(c => length(c.defaultValue))
        .sortWith { case (a, b) => a > b }
        .head
      if (maxLength > 20) 20 else if (maxLength < 7) 7 else maxLength
    }
    val maxDescriptionLength = {
      val maxLength = columns
        .map(c => length(c.description))
        .sortWith { case (a, b) => a > b }
        .head
      if (maxLength < 11) 11 else if (maxLength > 40) 40 else maxLength
    }

    "\n" +
      "Table: " + nameWithSchema + {
        if (description == null || description.trim.isEmpty) ""
        else if (length(description) > 120)
          " (" + take(description, 118) + "..)"
        else " (" + withoutCRLF(description) + ")"
      } + "\n" +
      "+-" + "-" * maxColumnNameLength + "-+-" + "-" * maxTypeNameLength + "-+------+-----+-" + "-" * maxDefaultValueLength + "-+-----------------+-" + "-" * maxDescriptionLength + "-+\n" +
      "| Field" + " " * (maxColumnNameLength - 5) + " | Type" + " " * (maxTypeNameLength - 4) + " | Null | Key | Default" + " " * (maxDefaultValueLength - 7) + " | Extra           | Description" + " " * (maxDescriptionLength - 11) + " |\n" +
      "+-" + "-" * maxColumnNameLength + "-+-" + "-" * maxTypeNameLength + "-+------+-----+-" + "-" * maxDefaultValueLength + "-+-----------------+-" + "-" * maxDescriptionLength + "-+\n" +
      columns.map { column =>
        val typeName =
          column.typeName + (if (column.size > 0) "(" + column.size + ")"
                             else "")
        val nullable = if (column.isRequired) "NO" else "YES"
        val key = if (column.isPrimaryKey) "PRI" else ""
        val defaultValue = {
          if (column.defaultValue == null) "NULL"
          else if (column.defaultValue.length > 20)
            column.defaultValue.take(18) + ".."
          else column.defaultValue
        }
        val extra = if (column.isAutoIncrement) "AUTO_INCREMENT" else ""
        val description = {
          if (column.description == null) ""
          else if (length(column.description) > 40)
            take(column.description, 38) + ".."
          else column.description
        }

        "| " + column.name + " " * (maxColumnNameLength - column.name.length) + " | " +
          typeName + " " * (maxTypeNameLength - typeName.length) + " | " +
          nullable + " " * (4 - nullable.length) + " | " +
          key + " " * (3 - key.length) + " | " +
          defaultValue + " " * (maxDefaultValueLength - defaultValue.length) + " | " +
          extra + " " * (15 - extra.length) + " | " +
          withoutCRLF(description) + " " * (maxDescriptionLength - length(
            description
          )) + " |\n"
      }.mkString +
      "+-" + "-" * maxColumnNameLength + "-+-" + "-" * maxTypeNameLength + "-+------+-----+-" + "-" * maxDefaultValueLength + "-+-----------------+-" + "-" * maxDescriptionLength + "-+\n" + {
        if (indices.nonEmpty) "Indexes:\n" + indices.map { index =>
          "  \"" + index.name + "\"" + (if (index.isUnique) " UNIQUE,"
                                        else "") + " (" + index.columnNames
            .mkString(", ") + ")" + "\n"
        }.mkString
        else ""
      } + {
        if (foreignKeys.nonEmpty) "Foreign Keys:\n" + foreignKeys.map { fk =>
          "  " + fk.name + " -> " + fk.foreignTableName + "(" + fk.foreignColumnName + ")" + "\n"
        }.mkString
        else ""
      }
  }

}
