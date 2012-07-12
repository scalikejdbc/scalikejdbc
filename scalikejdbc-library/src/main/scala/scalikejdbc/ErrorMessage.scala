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
private[scalikejdbc] object ErrorMessage {

  val CONNECTION_POOL_IS_NOT_YET_INITIALIZED = "Connection pool is not yet initialized."

  val CANNOT_START_A_NEW_TRANSACTION = "Cannot start a new transaction."

  val CANNOT_EXECUTE_IN_READ_ONLY_SESSION = "Cannot execute this operation in a readOnly session."

  val TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active."

  val IMPLICIT_DB_INSTANCE_REQUIRED = "An instance of scalikejdbc.DB is required implicitly."

  val INVALID_CURSOR_POSITION = "Invalid cursor position."

  val FAILED_TO_RETRIEVE_GENERATED_KEY = "Failed to retrieve the generated key."

  val BINDING_PARAMETER_IS_MISSING = "Binding parameter is missing."

  val BINDING_IS_IGNORED = "Passed named parameter is ignored."

  val NO_CONNECTION_POOL_CONTEXT = "No connection pool context exists."

  val UNKNOWN_CONNECTION_POOL_CONTEXT = "Unknown connection pool context is specified."

}
