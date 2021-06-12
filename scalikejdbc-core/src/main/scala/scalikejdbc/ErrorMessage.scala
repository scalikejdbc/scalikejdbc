package scalikejdbc

/**
 * Error messages
 */
private[scalikejdbc] object ErrorMessage {

  val THIS_IS_A_BUG =
    "If you see this message, it's a ScalikeJDBC's bug. Please report us with stacktrace."

  val CONNECTION_POOL_IS_NOT_YET_INITIALIZED =
    "Connection pool is not yet initialized."

  val CANNOT_START_A_NEW_TRANSACTION = "Cannot start a new transaction."

  val CANNOT_EXECUTE_IN_READ_ONLY_SESSION =
    "Cannot execute this operation in a readOnly session."

  val TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active."

  val IMPLICIT_DB_INSTANCE_REQUIRED =
    "An instance of scalikejdbc.DB is required implicitly."

  val INVALID_CURSOR_POSITION = "Invalid cursor position."

  val FAILED_TO_RETRIEVE_GENERATED_KEY = "Failed to retrieve the generated key."

  val BINDING_PARAMETER_IS_MISSING = "Binding parameter is missing."

  val BINDING_IS_IGNORED = "Passed named parameter is ignored."

  val NO_CONNECTION_POOL_CONTEXT = "No connection pool context exists."

  val UNKNOWN_CONNECTION_POOL_CONTEXT =
    "Unknown connection pool context is specified."

  val INVALID_COLUMN_NAME = "Invalid column name."

  val INVALID_ONE_TO_ONE_RELATION =
    "one-to-one relation is expected but it seems to be a one-to-many relationship."

  val INVALID_CONNECTION_POOL_FACTORY_NAME =
    "Invalid connection pool factory name."

}
