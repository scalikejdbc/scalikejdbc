package scalikejdbc.mapper

/*
 * Copyright 2012 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import scalikejdbc._
import scalikejdbc.{ ResultSetTraversable => RSTraversable }

case class Model(url: String, username: String, password: String) {

  ConnectionPool.singleton(url, username, password)

  private def columnName(implicit rs: WrappedResultSet): String = rs.string("COLUMN_NAME")

  private def columnDataType(implicit rs: WrappedResultSet): Int = rs.string("DATA_TYPE").toInt

  private def isNotNull(implicit rs: WrappedResultSet): Boolean = {
    val isNullable = rs.string("IS_NULLABLE")
    isNullable == "NO" || isNullable == "N"
  }

  private def isAutoIncrement(implicit rs: WrappedResultSet): Boolean = try {
    val isAutoIncrement = rs.string("IS_AUTOINCREMENT")
    isAutoIncrement == "YES" || isAutoIncrement == "Y"
  } catch { case e => false }

  def table(schema: String = null, tableName: String): Option[Table] = {
    val catalog = null
    val _schema = if (schema == null || schema.size == 0) null else schema
    DB readOnlyWithConnection { conn =>
      val meta = conn.getMetaData
      new RSTraversable(meta.getColumns(catalog, _schema, tableName, "%"))
        .map { implicit rs => Column(columnName, columnDataType, isNotNull, isAutoIncrement) }.toList.distinct match {
          case Nil => None
          case allColumns =>
            Some(Table(
              name = tableName,
              allColumns = allColumns,
              autoIncrementColumns = allColumns.filter(c => c.isAutoIncrement).distinct,
              primaryKeyColumns = {
                new RSTraversable(meta.getPrimaryKeys(catalog, _schema, tableName))
                  .flatMap { implicit rs =>
                    allColumns.find(column => column.name == columnName)
                  }.toList.distinct
              }
            ))
        }
    }
  }

}

