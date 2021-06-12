/*
 * Copyright 2014 Artem Vlasov
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
 * BoneCP Connection Pool
 *
 * @see [[https://github.com/wwadge/bonecp]]
 */
class BoneCPConnectionPool(
  override val url: String,
  override val user: String,
  password: String,
  override val settings: ConnectionPoolSettings = ConnectionPoolSettings()
) extends ConnectionPool(url, user, password, settings) {

  import com.jolbox.bonecp.BoneCPDataSource

  private[this] val _dataSource = {
    val ds = new BoneCPDataSource()

    ds.setJdbcUrl(url)
    ds.setUsername(user)
    ds.setPassword(password)

    ds.setMinConnectionsPerPartition(settings.initialSize)
    ds.setMaxConnectionsPerPartition(settings.maxSize)
    ds.setConnectionTimeoutInMs(settings.connectionTimeoutMillis)

    if (settings.validationQuery != null) {
      ds.getConfig.setInitSQL(settings.validationQuery)
    }

    ds
  }

  override def dataSource: DataSource = _dataSource

  override def borrow(): Connection = dataSource.getConnection()

  override def numActive: Int = _dataSource.getPool.getTotalLeased

  override def numIdle: Int = _dataSource.getPool.getTotalFree

  override def maxActive: Int = _dataSource.getMaxConnectionsPerPartition

  override def maxIdle: Int = _dataSource.getMinConnectionsPerPartition

  override def close(): Unit = _dataSource.close()

}
