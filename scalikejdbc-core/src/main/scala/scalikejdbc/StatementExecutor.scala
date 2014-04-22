/*
 * Copyright 2012 Kazuhiro Sera
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

import java.sql.PreparedStatement

/**
 * Companion object.
 */
object StatementExecutor {

  type MutableList[A] = collection.mutable.MutableList[A]

  val eol = System.getProperty("line.separator")

  private trait Executor {
    def apply[A](execute: () => A): A
  }
  private class NakedExecutor extends Executor {
    override def apply[A](execute: () => A): A = execute()
  }

}

/**
 * java.sql.Statement Executor.
 *
 * @param underlying preparedStatement
 * @param template SQL template
 * @param singleParams parameters for single execution (= not batch execution)
 * @param isBatch is batch flag
 */
case class StatementExecutor(underlying: PreparedStatement, template: String, singleParams: Seq[Any] = Nil, isBatch: Boolean = false)
    extends LogSupport
    with UnixTimeInMillisConverterImplicits {

  import StatementExecutor._

  private[this] lazy val batchParamsList = new MutableList[Seq[Any]]

  initialize()

  /**
   * Initializes this instance.
   */
  private def initialize() {
    bindParams(singleParams)
    if (isBatch) {
      batchParamsList.clear()
    }
  }

  /**
   * Binds parameters to the underlying java.sql.PreparedStatement object.
   * @param params parameters
   */
  def bindParams(params: Seq[Any]): Unit = {
    val paramsWithIndices = params.map {
      case option: Option[_] => option.orNull[Any]
      case other => other
    }.zipWithIndex

    for ((param, idx) <- paramsWithIndices; i = idx + 1) {
      param match {
        case null => underlying.setObject(i, null)
        case p: java.sql.Array => underlying.setArray(i, p)
        case p: BigDecimal => underlying.setBigDecimal(i, p.bigDecimal)
        case p: Boolean => underlying.setBoolean(i, p)
        case p: Byte => underlying.setByte(i, p)
        case p: java.sql.Date => underlying.setDate(i, p)
        case p: Double => underlying.setDouble(i, p)
        case p: Float => underlying.setFloat(i, p)
        case p: Int => underlying.setInt(i, p)
        case p: Long => underlying.setLong(i, p)
        case p: Short => underlying.setShort(i, p)
        case p: java.sql.SQLXML => underlying.setSQLXML(i, p)
        case p: String => underlying.setString(i, p)
        case p: java.sql.Time => underlying.setTime(i, p)
        case p: java.sql.Timestamp => underlying.setTimestamp(i, p)
        case p: java.net.URL => underlying.setURL(i, p)
        case p: java.util.Date => underlying.setTimestamp(i, p.toSqlTimestamp)
        case p: org.joda.time.DateTime => underlying.setTimestamp(i, p.toDate.toSqlTimestamp)
        case p: org.joda.time.LocalDateTime => underlying.setTimestamp(i, p.toDate.toSqlTimestamp)
        case p: org.joda.time.LocalDate => underlying.setDate(i, p.toDate.toSqlDate)
        case p: org.joda.time.LocalTime => underlying.setTime(i, p.toSqlTime)
        case p: java.io.InputStream => underlying.setBinaryStream(i, p)
        case p => {
          log.debug("The parameter(" + p + ") is bound as an Object.")
          underlying.setObject(i, p)
        }
      }
    }
    if (isBatch) {
      batchParamsList += params
    }
  }

  /**
   * SQL String value
   */
  private[this] lazy val sqlString: String = {

    def singleSqlString(params: Seq[Any]): String = {

      def toPrintable(param: Any): String = {
        def normalize(param: Any): Any = {
          param match {
            case None => null
            case Some(p) => normalize(p)
            case p: String => p
            case p: java.util.Date => p.toSqlTimestamp.toString
            case p: org.joda.time.DateTime => p.toDate.toSqlTimestamp.toString
            case p: org.joda.time.LocalDateTime => p.toDate.toSqlTimestamp
            case p: org.joda.time.LocalDate => p.toDate.toSqlDate
            case p: org.joda.time.LocalTime => p.toSqlTime
            case p => p
          }
        }
        (normalize(param) match {
          case null => "null"
          case result: String if result.size > 100 => "'" + result.take(100) + "... (" + result.size + ")" + "'"
          case result: String => "'" + result + "'"
          case result => result.toString
        }).replaceAll("\r", "\\\\r")
          .replaceAll("\n", "\\\\n")
      }

      var i = 0
      def trimSpaces(s: String, i: Int = 0): String = i match {
        case i if i > 10 => s
        case i => trimSpaces(s.replaceAll("  ", " "), i + 1)
      }

      var isInsideOfText = false
      val sql = SQLTemplateParser.trimComments(trimSpaces(template
        .replaceAll("\r", " ")
        .replaceAll("\n", " ")
        .replaceAll("\t", " ")))
        .map { c =>
          if (c == '\'') {
            isInsideOfText = !isInsideOfText
            c
          } else if (!isInsideOfText && c == '?') {
            i += 1
            if (params.size >= i) {
              toPrintable(params(i - 1))
            } else {
              // In this case, SQLException will be thrown later.
              // At least, throwing java.lang.IndexOutOfBoundsException here is meaningless.
              c
            }
          } else {
            c
          }
        }.mkString

      try {
        if (GlobalSettings.sqlFormatter.formatter.isDefined) {
          val formatter = GlobalSettings.sqlFormatter.formatter.get
          formatter.format(sql)
        } else {
          sql
        }
      } catch {
        case e: Exception =>
          log.debug("Catched an exception when formatting SQL because of " + e.getMessage)
          sql
      }
    }

    if (isBatch) {
      if (batchParamsList.size > 20) {
        batchParamsList.take(20).map(params => singleSqlString(params)).mkString(";" + eol + "   ") + ";" + eol +
          "   ... (total: " + batchParamsList.size + " times)"
      } else {
        batchParamsList.map(params => singleSqlString(params)).mkString(";" + eol + "   ")
      }
    } else {
      singleSqlString(singleParams)
    }

  }

  /**
   * Returns stack trace information as String value
   * @return stack trace
   */
  private[this] def stackTraceInformation: String = {

    val stackTrace = Thread.currentThread.getStackTrace
    val lines = (if (GlobalSettings.loggingSQLAndTime.printUnprocessedStackTrace) {
      stackTrace.tail
    } else {
      stackTrace.dropWhile { trace =>
        val className = trace.getClassName
        className != getClass.toString &&
          (className.startsWith("java.lang.") || className.startsWith("scalikejdbc."))
      }
    }).take(GlobalSettings.loggingSQLAndTime.stackTraceDepth).map { trace => "    " + trace.toString }

    "  [Stack Trace]" + eol +
      "    ..." + eol +
      lines.mkString(eol) + eol +
      "    ..." + eol
  }

  /**
   * Logging SQL and timing (this trait depends on this instance)
   */
  private[this] trait LoggingSQLAndTiming extends Executor with LogSupport {

    abstract override def apply[A](execute: () => A): A = {
      import GlobalSettings.loggingSQLAndTime

      def messageInSingleLine(spentMillis: Long): String = "[SQL Execution] " + sqlString + "; (" + spentMillis + " ms)"
      def messageInMultiLines(spentMillis: Long): String = {
        "SQL execution completed" + eol +
          eol +
          "  [SQL Execution]" + eol +
          "   " + sqlString + "; (" + spentMillis + " ms)" + eol +
          eol +
          stackTraceInformation
      }

      val before = System.currentTimeMillis
      val result = super.apply(execute)
      val after = System.currentTimeMillis
      val spentMillis = after - before

      // logging SQL and time
      if (loggingSQLAndTime.enabled) {
        if (loggingSQLAndTime.warningEnabled &&
          spentMillis >= loggingSQLAndTime.warningThresholdMillis) {
          if (loggingSQLAndTime.singleLineMode) {
            log.withLevel(loggingSQLAndTime.warningLogLevel)(messageInSingleLine(spentMillis))
          } else {
            log.withLevel(loggingSQLAndTime.warningLogLevel)(messageInMultiLines(spentMillis))
          }
        } else {
          if (loggingSQLAndTime.singleLineMode) {
            log.withLevel(loggingSQLAndTime.logLevel)(messageInSingleLine(spentMillis))
          } else {
            log.withLevel(loggingSQLAndTime.logLevel)(messageInMultiLines(spentMillis))
          }
        }
      }
      // call event handler
      GlobalSettings.queryCompletionListener.apply(template, singleParams, spentMillis)
      // result from super.apply()
      result
    }
  }

  private[this] trait LoggingSQLIfFailed extends Executor with LogSupport {

    abstract override def apply[A](execute: () => A): A = try {
      super.apply(execute)
    } catch {
      case e: Exception =>
        if (GlobalSettings.loggingSQLErrors) {
          if (GlobalSettings.loggingSQLAndTime.singleLineMode) {
            log.error("[SQL Execution Failed] " + sqlString + " (Reason: " + e.getMessage + ")")
          } else {
            log.error("SQL execution failed (Reason: " + e.getMessage + "):" + eol + eol + "   " + sqlString + eol)
          }
        } else {
          log.debug("Logging SQL errors is disabled.")
        }
        // call event handler
        GlobalSettings.queryFailureListener.apply(template, singleParams, e)

        throw e
    }
  }

  /**
   * Executes SQL statement
   */
  private[this] val statementExecute = new NakedExecutor with LoggingSQLAndTiming with LoggingSQLIfFailed

  def addBatch(): Unit = underlying.addBatch()

  def execute(): Boolean = statementExecute(() => underlying.execute())

  def execute(x1: String): Boolean = statementExecute(() => underlying.execute(x1))

  def execute(x1: String, x2: Array[Int]): Boolean = statementExecute(() => underlying.execute(x1, x2))

  def execute(x1: String, x2: Array[String]): Boolean = statementExecute(() => underlying.execute(x1, x2))

  def execute(x1: String, x2: Int): Boolean = statementExecute(() => underlying.execute(x1, x2))

  def executeBatch(): Array[Int] = statementExecute(() => underlying.executeBatch())

  def executeQuery(): java.sql.ResultSet = statementExecute(() => underlying.executeQuery())

  def executeQuery(x1: String): java.sql.ResultSet = statementExecute(() => underlying.executeQuery(x1))

  def executeUpdate(): Int = statementExecute(() => underlying.executeUpdate())

  def executeUpdate(x1: String): Int = statementExecute(() => underlying.executeUpdate(x1))

  def executeUpdate(x1: String, x2: Array[Int]): Int = statementExecute(() => underlying.executeUpdate(x1, x2))

  def executeUpdate(x1: String, x2: Array[String]): Int = statementExecute(() => underlying.executeUpdate(x1, x2))

  def executeUpdate(x1: String, x2: Int): Int = statementExecute(() => underlying.executeUpdate(x1, x2))

  def close() = underlying.close()

}
