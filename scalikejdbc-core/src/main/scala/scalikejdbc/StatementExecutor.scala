package scalikejdbc

import java.sql.PreparedStatement

import org.slf4j.LoggerFactory
import scala.collection.compat._
import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import JavaUtilDateConverterImplicits._

/**
 * Companion object.
 */
object StatementExecutor {

  val eol: String = System.getProperty("line.separator")

  private trait Executor {
    def apply[A](execute: () => A): A
  }
  private class NakedExecutor extends Executor {
    override def apply[A](execute: () => A): A = execute()
  }

  private val LocalDateEpoch = java.time.LocalDate.ofEpochDay(0)

  object PrintableQueryBuilder extends PrintableQueryBuilder {
    // Find ? placeholders, but ignore ?? because that's an escaped question mark.
    private val substituteRegex = "(?<!\\?)(\\?)(?!\\?)".r

    /**
     * Logger
     */
    private val log = new Log(
      LoggerFactory.getLogger(classOf[PrintableQueryBuilder])
    )

  }

  trait PrintableQueryBuilder {

    import PrintableQueryBuilder._

    def build(
      template: String,
      settingsProvider: SettingsProvider,
      params: collection.Seq[Any]
    ): String = {
      try {
        def toPrintable(param: Any): String = {
          @annotation.tailrec
          def normalize(param: Any): Any = {
            param match {
              case null               => null
              case ParameterBinder(v) => normalize(v)
              case None               => null
              case Some(p)            => normalize(p)
              case p: String          => p
              case p: java.util.Date  => p.toSqlTimestamp.toString
              case p =>
                ClassNameUtil.getClassName(param.getClass) match {
                  case "org.joda.time.DateTime" =>
                    param
                      .asInstanceOf[{ def toDate: java.util.Date }]
                      .toDate
                      .toSqlTimestamp
                      .toString
                  case "org.joda.time.LocalDateTime" =>
                    param
                      .asInstanceOf[{ def toDate: java.util.Date }]
                      .toDate
                      .toSqlTimestamp
                      .toString
                  case "org.joda.time.LocalDate" =>
                    param
                      .asInstanceOf[{ def toDate: java.util.Date }]
                      .toDate
                      .toSqlDate
                      .toString
                  case "org.joda.time.LocalTime" =>
                    val millis = param
                      .asInstanceOf[{
                          def toDateTimeToday: { def getMillis: Long }
                        }
                      ]
                      .toDateTimeToday
                      .getMillis
                    new java.sql.Time(millis).toSqlTime.toString
                  case _ => p
                }
            }
          }

          (normalize(param) match {
            case null => "null"
            case result: String =>
              settingsProvider
                .loggingSQLAndTime(GlobalSettings.loggingSQLAndTime)
                .maxColumnSize
                .collect {
                  case maxSize if result.length > maxSize =>
                    "'" + result.take(
                      maxSize
                    ) + "... (" + result.length + ")" + "'"
                }
                .getOrElse {
                  "'" + result + "'"
                }
            case result => result.toString
          }).replaceAll("\r", "\\\\r")
            .replaceAll("\n", "\\\\n")
        }

        var i = 0
        @annotation.tailrec
        def trimSpaces(s: String, i: Int = 0): String = i match {
          case i if i > 10 => s
          case i           => trimSpaces(s.replaceAll("  ", " "), i + 1)
        }

        val sqlWithPlaceholders = trimSpaces(
          SQLTemplateParser
            .trimComments(template)
            .replaceAll("[\r\n\t]", " ")
        )

        sqlWithPlaceholders
          .split('\'')
          .zipWithIndex
          .map {
            // Even numbered parts are outside quotes, odd numbered are inside
            case (target, quoteCount) if quoteCount % 2 == 0 => {
              substituteRegex.replaceAllIn(
                target,
                m => {
                  i += 1
                  if (params.sizeIs >= i) {
                    toPrintable(params(i - 1))
                      .replace("\\", "\\\\")
                      .replace("$", "\\$")
                  } else {
                    // In this case, SQLException will be thrown later.
                    // At least, throwing java.lang.IndexOutOfBoundsException here is meaningless.
                    m.source.toString()
                  }
                }
              )
            }
            case (s, quoteCount) if quoteCount % 2 == 1 =>
              // If the statement is valid, we can always expect an odd number of elements
              // Thus, we can add two quotes here.
              "'" + s + "'"
            case (s, _) => s
          }
          .mkString

      } catch {
        case NonFatal(e) =>
          log.debug(
            s"Failed to build a printable SQL statement with ${template}, params: ${params}",
            e
          )
          template
      }
    }
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
case class StatementExecutor(
  underlying: PreparedStatement,
  template: String,
  connectionAttributes: DBConnectionAttributes,
  singleParams: collection.Seq[Any] = Nil,
  tags: collection.Seq[String] = Nil,
  isBatch: Boolean = false,
  settingsProvider: SettingsProvider = SettingsProvider.default
) extends LogSupport
  with AutoCloseable {

  import StatementExecutor._

  private[this] lazy val batchParamsList =
    scala.collection.mutable.ArrayBuffer.empty[collection.Seq[Any]]

  initialize()

  /**
   * Initializes this instance.
   */
  private def initialize(): Unit = {
    bindParams(singleParams)
    if (isBatch) {
      batchParamsList.clear()
    }
  }

  /**
   * Binds parameters to the underlying java.sql.PreparedStatement object.
   * @param params parameters
   */
  def bindParams(params: collection.Seq[Any]): Unit = {
    val paramsWithIndices = params.map {
      case option: Option[_] => option.orNull[Any]
      case other             => other
    }.zipWithIndex

    for ((param, idx) <- paramsWithIndices) {
      bind(param, idx + 1)
    }
    if (isBatch) {
      batchParamsList += params
    }
  }

  @annotation.tailrec
  private[this] def bind(param: Any, i: Int): Unit = {
    param match {
      case null                             => underlying.setObject(i, null)
      case AsIsParameterBinder(None)        => bind(null, i)
      case AsIsParameterBinder(Some(value)) => bind(value, i)
      case AsIsParameterBinder(value)       => bind(value, i)
      case binder: ParameterBinder          => binder(underlying, i)
      case p: java.sql.Array                => underlying.setArray(i, p)
      case p: BigDecimal => underlying.setBigDecimal(i, p.bigDecimal)
      case p: BigInt =>
        underlying.setBigDecimal(i, new java.math.BigDecimal(p.bigInteger))
      case p: Boolean            => underlying.setBoolean(i, p)
      case p: Byte               => underlying.setByte(i, p)
      case p: java.sql.Date      => underlying.setDate(i, p)
      case p: Double             => underlying.setDouble(i, p)
      case p: Float              => underlying.setFloat(i, p)
      case p: Int                => underlying.setInt(i, p)
      case p: Long               => underlying.setLong(i, p)
      case p: Short              => underlying.setShort(i, p)
      case p: java.sql.SQLXML    => underlying.setSQLXML(i, p)
      case p: String             => underlying.setString(i, p)
      case p: java.sql.Time      => underlying.setTime(i, p)
      case p: java.sql.Timestamp => underlying.setTimestamp(i, p)
      case p: java.net.URL       => underlying.setURL(i, p)
      case p: java.util.Date     => underlying.setTimestamp(i, p.toSqlTimestamp)
      case p: java.time.ZonedDateTime =>
        underlying.setTimestamp(i, java.sql.Timestamp.from(p.toInstant))
      case p: java.time.OffsetDateTime =>
        underlying.setTimestamp(i, java.sql.Timestamp.from(p.toInstant))
      case p: java.time.Instant =>
        underlying.setTimestamp(i, java.sql.Timestamp.from(p))
      case p: java.time.LocalDateTime =>
        underlying.setTimestamp(i, java.sql.Timestamp.valueOf(p))
      case p: java.time.LocalDate =>
        underlying.setDate(i, java.sql.Date.valueOf(p))
      case p: java.time.LocalTime =>
        val millis = p
          .atDate(StatementExecutor.LocalDateEpoch)
          .atZone(java.time.ZoneId.systemDefault)
          .toInstant
          .toEpochMilli
        val time = new java.sql.Time(millis)
        underlying.setTime(i, time)
      case p: java.io.InputStream => underlying.setBinaryStream(i, p)
      case p =>
        ClassNameUtil.getClassName(param.getClass) match {
          case "org.joda.time.DateTime" =>
            val t = p
              .asInstanceOf[{ def toDate: java.util.Date }]
              .toDate
              .toSqlTimestamp
            underlying.setTimestamp(i, t)
          case "org.joda.time.LocalDateTime" =>
            val t = p
              .asInstanceOf[{ def toDate: java.util.Date }]
              .toDate
              .toSqlTimestamp
            underlying.setTimestamp(i, t)
          case "org.joda.time.LocalDate" =>
            val t =
              p.asInstanceOf[{ def toDate: java.util.Date }].toDate.toSqlDate
            underlying.setDate(i, t)
          case "org.joda.time.LocalTime" =>
            val millis = p
              .asInstanceOf[{ def toDateTimeToday: { def getMillis: Long } }]
              .toDateTimeToday
              .getMillis
            underlying.setTime(i, new java.sql.Time(millis))
          case _ =>
            log.debug("The parameter(" + p + ") is bound as an Object.")
            underlying.setObject(i, p)
        }
    }
  }

  /**
   * SQL String value
   */
  private[this] lazy val sqlString: String = {

    def singleSqlString(params: collection.Seq[Any]): String = {

      val sql = PrintableQueryBuilder.build(template, settingsProvider, params)

      try {
        settingsProvider
          .sqlFormatter(GlobalSettings.sqlFormatter)
          .formatter match {
          case Some(formatter) =>
            formatter.format(sql)
          case None =>
            sql
        }
      } catch {
        case e: Exception =>
          log.debug(
            "Caught an exception when formatting SQL because of " + e.getMessage
          )
          sql
      }
    }

    if (isBatch) {
      settingsProvider
        .loggingSQLAndTime(GlobalSettings.loggingSQLAndTime)
        .maxBatchParamSize
        .collect {
          case maxSize if batchParamsList.sizeIs > maxSize =>
            batchParamsList
              .take(maxSize)
              .map(params => singleSqlString(params))
              .mkString(";" + eol + "   ") + ";" + eol +
              "   ... (total: " + batchParamsList.size + " times)"
        }
        .getOrElse {
          batchParamsList
            .map(params => singleSqlString(params))
            .mkString(";" + eol + "   ")
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
    val loggingSQLAndTime =
      settingsProvider.loggingSQLAndTime(GlobalSettings.loggingSQLAndTime)

    val stackTrace = Thread.currentThread.getStackTrace
    val lines = (if (loggingSQLAndTime.printUnprocessedStackTrace) {
                   stackTrace.tail
                 } else {
                   stackTrace.dropWhile { trace =>
                     val className = trace.getClassName
                     className != getClass.toString &&
                     (className.startsWith("java.lang.") || className
                       .startsWith("scalikejdbc."))
                   }
                 }).take(loggingSQLAndTime.stackTraceDepth).map { trace =>
      "    " + trace.toString
    }

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
      val loggingSQLAndTime =
        settingsProvider.loggingSQLAndTime(GlobalSettings.loggingSQLAndTime)

      def messageInSingleLine(spentMillis: Long): String =
        "[SQL Execution] " + sqlString + "; (" + spentMillis + " ms)"
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
        if (
          loggingSQLAndTime.warningEnabled &&
          spentMillis >= loggingSQLAndTime.warningThresholdMillis
        ) {
          if (loggingSQLAndTime.singleLineMode) {
            log.withLevel(loggingSQLAndTime.warningLogLevel)(
              messageInSingleLine(spentMillis)
            )
          } else {
            log.withLevel(loggingSQLAndTime.warningLogLevel)(
              messageInMultiLines(spentMillis)
            )
          }
        } else {
          if (loggingSQLAndTime.singleLineMode) {
            log.withLevel(loggingSQLAndTime.logLevel)(
              messageInSingleLine(spentMillis)
            )
          } else {
            log.withLevel(loggingSQLAndTime.logLevel)(
              messageInMultiLines(spentMillis)
            )
          }
        }
      }
      // call event handler
      settingsProvider
        .queryCompletionListener(GlobalSettings.queryCompletionListener)
        .apply(template, singleParams, spentMillis)
      settingsProvider
        .taggedQueryCompletionListener(
          GlobalSettings.taggedQueryCompletionListener
        )
        .apply(template, singleParams, spentMillis, tags)

      // result from super.apply()
      result
    }
  }

  private[this] trait LoggingSQLIfFailed extends Executor with LogSupport {

    abstract override def apply[A](execute: () => A): A = try {
      super.apply(execute)
    } catch {
      case e: Exception =>
        if (
          settingsProvider.loggingSQLErrors(GlobalSettings.loggingSQLErrors)
        ) {
          if (
            settingsProvider
              .loggingSQLAndTime(GlobalSettings.loggingSQLAndTime)
              .singleLineMode
          ) {
            log.error(
              "[SQL Execution Failed] " + sqlString + " (Reason: " + e.getMessage + ")"
            )
          } else {
            log.error(
              "SQL execution failed (Reason: " + e.getMessage + "):" + eol + eol + "   " + sqlString + eol
            )
          }
        } else {
          log.debug("Logging SQL errors is disabled.")
        }
        // call event handler
        settingsProvider
          .queryFailureListener(GlobalSettings.queryFailureListener)
          .apply(template, singleParams, e)
        settingsProvider
          .taggedQueryFailureListener(GlobalSettings.taggedQueryFailureListener)
          .apply(template, singleParams, e, tags)

        throw e
    }
  }

  /**
   * Executes SQL statement
   */
  private[this] val statementExecute = new NakedExecutor
    with LoggingSQLAndTiming
    with LoggingSQLIfFailed

  def generatedKeysResultSet: java.sql.ResultSet = underlying.getGeneratedKeys

  def addBatch(): Unit = underlying.addBatch()

  def execute(): Boolean = statementExecute(() => underlying.execute())

  def execute(x1: String): Boolean =
    statementExecute(() => underlying.execute(x1))

  def execute(x1: String, x2: Array[Int]): Boolean =
    statementExecute(() => underlying.execute(x1, x2))

  def execute(x1: String, x2: Array[String]): Boolean =
    statementExecute(() => underlying.execute(x1, x2))

  def execute(x1: String, x2: Int): Boolean =
    statementExecute(() => underlying.execute(x1, x2))

  def executeBatch(): Array[Int] =
    statementExecute(() => underlying.executeBatch())

  def executeLargeBatch(): Array[Long] =
    statementExecute(() => underlying.executeLargeBatch())

  def executeQuery(): java.sql.ResultSet =
    statementExecute(() => underlying.executeQuery())

  def executeQuery(x1: String): java.sql.ResultSet =
    statementExecute(() => underlying.executeQuery(x1))

  def executeUpdate(): Int = statementExecute(() => underlying.executeUpdate())

  def executeUpdate(x1: String): Int =
    statementExecute(() => underlying.executeUpdate(x1))

  def executeUpdate(x1: String, x2: Array[Int]): Int =
    statementExecute(() => underlying.executeUpdate(x1, x2))

  def executeUpdate(x1: String, x2: Array[String]): Int =
    statementExecute(() => underlying.executeUpdate(x1, x2))

  def executeUpdate(x1: String, x2: Int): Int =
    statementExecute(() => underlying.executeUpdate(x1, x2))

  def executeLargeUpdate(): Long =
    statementExecute(() => underlying.executeLargeUpdate())

  def executeLargeUpdate(sql: String): Long =
    statementExecute(() => underlying.executeLargeUpdate(sql))

  def executeLargeUpdate(sql: String, columnIndexes: Array[Int]): Long =
    statementExecute(() => underlying.executeLargeUpdate(sql, columnIndexes))

  def executeLargeUpdate(sql: String, columnNames: Array[String]): Long =
    statementExecute(() => underlying.executeLargeUpdate(sql, columnNames))

  def executeLargeUpdate(sql: String, autoGeneratedKeys: Int): Long =
    statementExecute(() =>
      underlying.executeLargeUpdate(sql, autoGeneratedKeys)
    )

  def close(): Unit = underlying.close()

}
