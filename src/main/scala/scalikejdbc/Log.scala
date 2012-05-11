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

import org.slf4j.Logger

class Log(logger: Logger) {

  // use var consciously to enable squeezing later
  var isDebugEnabled: Boolean = logger.isDebugEnabled
  var isInfoEnabled: Boolean = logger.isInfoEnabled
  var isWarnEnabled: Boolean = logger.isWarnEnabled
  var isErrorEnabled: Boolean = logger.isErrorEnabled

  def withLevel(level: Symbol)(msg: => String, e: Throwable = null): Unit = {
    level match {
      case 'debug | 'DEBUG => debug(msg)
      case 'info | 'INFO => info(msg)
      case 'warn | 'WARN => warn(msg)
      case 'error | 'ERROR => error(msg)
      case _ => // nothing to do
    }
  }

  def debug(msg: => String): Unit = {
    if (isDebugEnabled && logger.isDebugEnabled) {
      logger.debug(msg)
    }
  }

  def debug(msg: => String, e: Throwable): Unit = {
    if (isDebugEnabled && logger.isDebugEnabled) {
      logger.debug(msg, e)
    }
  }

  def info(msg: => String): Unit = {
    if (isInfoEnabled && logger.isInfoEnabled) {
      logger.info(msg)
    }
  }

  def info(msg: => String, e: Throwable): Unit = {
    if (isInfoEnabled && logger.isInfoEnabled) {
      logger.info(msg, e)
    }
  }

  def warn(msg: => String): Unit = {
    if (isWarnEnabled && logger.isWarnEnabled) {
      logger.warn(msg)
    }
  }

  def warn(msg: => String, e: Throwable): Unit = {
    if (isWarnEnabled && logger.isWarnEnabled) {
      logger.warn(msg, e)
    }
  }

  def error(msg: => String): Unit = {
    if (isErrorEnabled && logger.isErrorEnabled) {
      logger.error(msg)
    }
  }

  def error(msg: => String, e: Throwable): Unit = {
    if (isErrorEnabled && logger.isErrorEnabled) {
      logger.error(msg, e)
    }
  }

}
