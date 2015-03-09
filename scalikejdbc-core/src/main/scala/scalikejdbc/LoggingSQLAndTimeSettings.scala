/*
 * Copyright 2011 - 2015 scalikejdbc.org
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
 * Settings for logging SQL and timing
 */
case class LoggingSQLAndTimeSettings(
  enabled: Boolean = true,
  singleLineMode: Boolean = false,
  printUnprocessedStackTrace: Boolean = false,
  stackTraceDepth: Int = 15,
  logLevel: Symbol = 'debug,
  warningEnabled: Boolean = false,
  warningThresholdMillis: Long = 3000L,
  warningLogLevel: Symbol = 'warn)
