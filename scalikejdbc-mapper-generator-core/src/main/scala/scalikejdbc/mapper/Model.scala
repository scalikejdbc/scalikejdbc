package scalikejdbc.mapper

import java.util.UUID
import scalikejdbc._
import scalikejdbc.{ ResultSetTraversable => RSTraversable }

case class Model(url: String, username: String, password: String) extends AutoCloseable {

  private[this] val poolName = UUID.randomUUID

  locally {
    ConnectionPool.add(
      name = poolName,
      url = url,
      user = username,
      password = password
    )
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

  private[this] def listAllTables(schema: String, types: List[String]): Seq[String] = {
    using(ConnectionPool.get(poolName).borrow()) { conn =>
      val meta = conn.getMetaData
      val (catalog, _schema) = {
        (schema, meta.getDatabaseProductName) match {
          case (null, _) => (null, null)
          case (s, _) if s.isEmpty => (null, null)
          case (s, "MySQL") => (s, null)
          case (s, _) => (null, s)
        }
      }
      new RSTraversable(meta.getTables(catalog, _schema, "%", types.toArray))
        .map { rs => rs.string("TABLE_NAME") }
        .toList
    }
  }

  def allTables(schema: String = null): Seq[Table] =
    listAllTables(schema, List("TABLE")).map(table(schema, _)).flatten

  def allViews(schema: String = null): Seq[Table] =
    listAllTables(schema, List("VIEW")).map(table(schema, _)).flatten

  def table(schema: String = null, tableName: String): Option[Table] = {
    val catalog = null
    val _schema = if (schema == null || schema.isEmpty) null else schema
    using(ConnectionPool.get(poolName).borrow()) { conn =>
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

  def close(): Unit = {
    ConnectionPool.close(poolName)
  }
}

