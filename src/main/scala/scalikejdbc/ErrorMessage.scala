/*
 * Copyright 2011 Kazuhiro Sera
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
 * Error messages
 */
object ErrorMessage {

  val CONNECTION_POOL_IS_NOT_YET_INITIALIZED = "Connection pool is not yet initialized!"

  val CANNOT_START_A_NEW_TRANSACTION = "Cannot start a new transaction!"

  val TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active!"

}
