/*
 * Copyright 2014 scalikejdbc.org
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

/**
 * ConnectionPoolFactoryRepository
 */
object ConnectionPoolFactoryRepository {

  val COMMONS_DBCP = "commons-dbcp"
  val BONECP = "bonecp"

  private[this] val factories = new scala.collection.concurrent.TrieMap[String, ConnectionPoolFactory]()

  factories.update(COMMONS_DBCP, CommonsConnectionPoolFactory)
  factories.update(BONECP, BoneCPConnectionPoolFactory)

  /**
   * Registers a connection pool factory to repository.
   */
  def add(name: String, factory: ConnectionPoolFactory): Unit = factories.update(name, factory)

  /**
   * Returns a connection pool factory.
   */
  def get(name: String): Option[ConnectionPoolFactory] = factories.get(name)

  /**
   * Removes a connection pool factory from repository.
   */
  def remove(name: String): Unit = factories.remove(name)

}
