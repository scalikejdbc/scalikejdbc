package scalikejdbc.mapper

import scalikejdbc._
import scalikejdbc.{ ResultSetTraversable => RSTraversable }

case class Model(url: String, username: String, password: String) {

  if (!ConnectionPool.isInitialized()) {
    ConnectionPool.singleton(url, username, password)
  }

  private def columnName(implicit rs: WrappedResultSet): String = rs.string("COLUMN_NAME")

  private def columnDataType(implicit rs: WrappedResultSet): Int = rs.string("DATA_TYPE").toInt

  private def isNotNull(implicit rs: WrappedResultSet): Boolean = {
    val isNullable = rs.string("IS_NULLABLE")
    isNullable == "NO" || isNullable == "N"
  }

  private def isAutoIncrement(implicit rs: WrappedResultSet): Boolean = try {
    val isAutoIncrement = rs.string("IS_AUTOINCREMENT")
    isAutoIncrement == "YES" || isAutoIncrement == "Y"
  } catch { case e: Exception => false }

  private[this] def listAllTables(schema: String): Seq[String] = {
    val catalog = null
    val _schema = if (schema == null || schema.size == 0) null else schema
    val types = Array("TABLE")
    DB readOnlyWithConnection { conn =>
      val meta = conn.getMetaData
      new RSTraversable(meta.getTables(catalog, _schema, "%", types))
        .map { rs => rs.string("TABLE_NAME") }
        .toList
    }
  }

  def allTables(schema: String = null): Seq[Table] =
    listAllTables(schema).map(table(schema, _)).flatten

  def table(schema: String = null, tableName: String): Option[Table] = {
    val catalog = null
    val _schema = if (schema == null || schema.size == 0) null else schema
    DB readOnlyWithConnection { conn =>
      val meta = conn.getMetaData
      new RSTraversable(meta.getColumns(catalog, _schema, tableName, "%"))
        .map { implicit rs => Column(columnName, columnDataType, isNotNull, isAutoIncrement) }
        .toList.distinct match {
          case Nil => None
          case allColumns =>
            Some(Table(
              schema = Option(schema),
              name = tableName,
              allColumns = allColumns,
              autoIncrementColumns = allColumns.filter(c => c.isAutoIncrement).distinct,
              primaryKeyColumns = {
                new RSTraversable(meta.getPrimaryKeys(catalog, _schema, tableName))
                  .flatMap { implicit rs => allColumns.find(column => column.name == columnName) }
                  .toList.distinct
              }
            ))
        }
    }
  }

}

