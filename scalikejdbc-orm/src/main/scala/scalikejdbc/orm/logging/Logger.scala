/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php
  Copyright (c) 2010, Brian M. Clapper
  All rights reserved.
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:
 * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 * Neither the names "clapper.org", "AVSL", nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
 */
package scalikejdbc.orm.logging

import org.slf4j.{ Logger => SLF4JLogger }

import scala.language.implicitConversions
import scala.reflect.{ ClassTag, classTag }

/**
 * A factory for retrieving an SLF4JLogger.
 */
object Logger {

  /**
   * The name associated with the root logger.
   */
  val RootLoggerName = SLF4JLogger.ROOT_LOGGER_NAME

  /**
   * Get the logger with the specified name. Use `RootName` to get the root logger.
   */
  def apply(name: String): Logger = new Logger(
    org.slf4j.LoggerFactory.getLogger(name)
  )

  /**
   * Get the logger for the specified class, using the class's fully
   * qualified name as the logger name.
   */
  def apply(cls: Class[?]): Logger = apply(cls.getName)

  /**
   * Get the logger for the specified class type, using the class's fully
   * qualified name as the logger name.
   */
  def apply[C: ClassTag](): Logger = apply(classTag[C].runtimeClass.getName)

  /**
   * Get the root logger.
   */
  def rootLogger = apply(RootLoggerName)

}

/**
 * Scala front-end to a SLF4J logger.
 */
class Logger(val logger: SLF4JLogger) {

  /**
   * Get the name associated with this logger.
   */
  @inline final def name = logger.getName

  /**
   * Determine whether trace logging is enabled.
   */
  @inline final def isTraceEnabled = logger.isTraceEnabled

  /**
   * Issue a trace logging message.
   */
  @inline final def trace(msg: => Any): Unit =
    if (isTraceEnabled) logger.trace(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   */
  @inline final def trace(msg: => Any, t: => Throwable): Unit =
    if (isTraceEnabled) logger.trace(msg, t)

  /**
   * Determine whether debug logging is enabled.
   */
  @inline final def isDebugEnabled = logger.isDebugEnabled

  /**
   * Issue a debug logging message.
   */
  @inline final def debug(msg: => Any): Unit =
    if (isDebugEnabled) logger.debug(msg.toString)

  /**
   * Issue a debug logging message, with an exception.
   */
  @inline final def debug(msg: => Any, t: => Throwable): Unit =
    if (isDebugEnabled) logger.debug(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  @inline final def isErrorEnabled = logger.isErrorEnabled

  /**
   * Issue a trace logging message.
   */
  @inline final def error(msg: => Any): Unit =
    if (isErrorEnabled) logger.error(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   */
  @inline final def error(msg: => Any, t: => Throwable): Unit =
    if (isErrorEnabled) logger.error(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  @inline final def isInfoEnabled = logger.isInfoEnabled

  /**
   * Issue a trace logging message.
   */
  @inline final def info(msg: => Any): Unit =
    if (isInfoEnabled) logger.info(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   */
  @inline final def info(msg: => Any, t: => Throwable): Unit =
    if (isInfoEnabled) logger.info(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  @inline final def isWarnEnabled = logger.isWarnEnabled

  /**
   * Issue a trace logging message.
   */
  @inline final def warn(msg: => Any): Unit =
    if (isWarnEnabled) logger.warn(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   */
  @inline final def warn(msg: => Any, t: => Throwable): Unit =
    if (isWarnEnabled) logger.warn(msg, t)

  /**
   * Converts any type to a String. In case the object is null, a null String is returned.
   * Otherwise the method `toString()` is called.
   */
  private implicit def _any2String(msg: Any): String = msg match {
    case null => "<null>"
    case _    => msg.toString
  }

}
