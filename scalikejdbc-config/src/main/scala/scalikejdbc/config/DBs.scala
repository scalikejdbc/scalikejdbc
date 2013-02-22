/*
 * Copyright 2013 Toshiyuki Takahashi, Kazuhiro Sera
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
package scalikejdbc.config

import scalikejdbc._

/**
 * DB configurator
 */
trait DBs { self: TypesafeConfigReader with Config =>

  def setup(dbName: Symbol = ConnectionPool.DEFAULT_NAME): Unit = {
    val JDBCSettings(driver, url, user, password) = TypesafeConfigReader.readJDBCSettings(dbName)
    val cpSettings = TypesafeConfigReader.readConnectionPoolSettings(dbName)
    Class.forName(driver)
    ConnectionPool.add(dbName, url, user, password, cpSettings)
  }

  def setupAll(): Unit = {
    TypesafeConfigReader.loadGlobalSettings()
    TypesafeConfigReader.dbNames.foreach { dbName => setup(Symbol(dbName)) }
  }

  def close(dbName: Symbol = ConnectionPool.DEFAULT_NAME): Unit = {
    ConnectionPool.close(dbName)
  }

  def closeAll(): Unit = {
    ConnectionPool.closeAll
  }

}

object DBs extends DBs with TypesafeConfigReader with StandardConfig
