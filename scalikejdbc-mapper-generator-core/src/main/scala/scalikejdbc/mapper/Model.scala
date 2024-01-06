package scalikejdbc.mapper

import java.sql.JDBCType
import java.util.UUID

import scalikejdbc._

case class Model(url: String, username: String, password: String)
  extends AutoCloseable {

  private[this] val poolName = UUID.randomUUID

  locally {
    ConnectionPool.add(
      name = poolName,
      url = url,
      user = username,
      password = password
    )
  }

  private def columnName(implicit rs: WrappedResultSet): String =
    rs.string("COLUMN_NAME")

  private def columnDataType(implicit rs: WrappedResultSet): JDBCType =
    JDBCType.valueOf(rs.string("DATA_TYPE").toInt)

  private def isNotNull(implicit rs: WrappedResultSet): Boolean = {
    val isNullable = rs.string("IS_NULLABLE")
    isNullable == "NO" || isNullable == "N"
  }

  private def isAutoIncrement(implicit rs: WrappedResultSet): Boolean = try {
    val isAutoIncrement = rs.string("IS_AUTOINCREMENT")
    isAutoIncrement == "YES" || isAutoIncrement == "Y"
  } catch { case e: Exception => false }

  private[this] def listAllTables(
    schema: String,
    types: List[String]
  ): collection.Seq[(String, String)] = {
    using(ConnectionPool.get(poolName).borrow()) { conn =>
      val meta = conn.getMetaData
      val (catalog, _schema) = {
        (schema, meta.getDatabaseProductName) match {
          case (null, _)           => (null, null)
          case (s, _) if s.isEmpty => (null, null)
          case (s, "MySQL")        => (s, null)
          case (s, _)              => (null, s)
        }
      }
      new ResultSetIterator(
        meta.getTables(catalog, _schema, "%", types.toArray)
      ).map(rs => (rs.string("TABLE_CAT"), rs.string("TABLE_NAME"))).toList
    }
  }

  def allTables(schema: String = null): collection.Seq[Table] =
    listAllTables(schema, List("TABLE")).flatMap(t => table(schema, t._2, t._1))

  def allViews(schema: String = null): collection.Seq[Table] =
    listAllTables(schema, List("VIEW")).flatMap(t => table(schema, t._2, t._1))

  def table(schema: String = null, tableName: String, catalog: String = null): Option[Table] = {
    val _schema = if (schema == null || schema.isEmpty) null else schema
    using(ConnectionPool.get(poolName).borrow()) { conn =>
      val meta = conn.getMetaData
      new ResultSetIterator(meta.getColumns(catalog, _schema, tableName, "%"))
        .map { implicit rs =>
          Column(columnName, columnDataType, isNotNull, isAutoIncrement)
        }
        .toList
        .distinct match {
        case Nil => None
        case allColumns =>
          Some(
            Table(
              name = tableName,
              allColumns = allColumns,
              autoIncrementColumns =
                allColumns.filter(_.isAutoIncrement).distinct,
              primaryKeyColumns = {
                new ResultSetIterator(
                  meta.getPrimaryKeys(catalog, _schema, tableName)
                )
                  .flatMap { implicit rs =>
                    allColumns.find(_.name == columnName)
                  }
                  .toList
                  .distinct
              },
              schema = Option(schema)
            )
          )
      }
    }
  }

  def close(): Unit = {
    ConnectionPool.close(poolName)
  }
}
