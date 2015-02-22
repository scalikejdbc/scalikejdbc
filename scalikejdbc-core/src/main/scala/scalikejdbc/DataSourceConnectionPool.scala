/*
 * Copyright 2013 Kazuhiro Sera
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
package scalikejdbc

import javax.sql.DataSource
import java.sql.Connection

/**
 * Connection Pool using external DataSource
 */
class DataSourceConnectionPool(override val dataSource: DataSource, settings: DataSourceConnectionPoolSettings = DataSourceConnectionPoolSettings())
    extends ConnectionPool("<external-data-source>", "<external-data-source>", "<external-data-source>", ConnectionPoolSettings(driverName = settings.driverName)) {

  override def borrow(): Connection = dataSource.getConnection()
}

/**
 * Connection Pool using external DataSource
 *
 * Note: Commons-DBCP doesn't support this API.
 */
class AuthenticatedDataSourceConnectionPool(
  override val dataSource: DataSource, override val user: String, password: String, settings: DataSourceConnectionPoolSettings = DataSourceConnectionPoolSettings())
    extends ConnectionPool("<external-data-source>", user, password, ConnectionPoolSettings(driverName = settings.driverName)) {

  override def borrow(): Connection = dataSource.getConnection(user, password)
}
