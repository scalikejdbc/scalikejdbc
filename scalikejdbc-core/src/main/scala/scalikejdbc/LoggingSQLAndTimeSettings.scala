package scalikejdbc

/**
 * Settings for logging SQL and timing
 */
case class LoggingSQLAndTimeSettings(
  enabled: Boolean = true,
  singleLineMode: Boolean = false,
  printUnprocessedStackTrace: Boolean = false,
  stackTraceDepth: Int = 15,
  logLevel: String = "debug",
  warningEnabled: Boolean = false,
  warningThresholdMillis: Long = 3000L,
  warningLogLevel: String = "warn",
  maxColumnSize: Option[Int] = Some(100),
  maxBatchParamSize: Option[Int] = Some(20)
)
