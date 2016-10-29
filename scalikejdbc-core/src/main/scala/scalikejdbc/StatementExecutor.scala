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
case class StatementExecutor(
    underlying: PreparedStatement,
    template: String,
    connectionAttributes: DBConnectionAttributes,
    singleParams: Seq[Any] = Nil,
    tags: Seq[String] = Nil,
    isBatch: Boolean = false
) extends LogSupport with UnixTimeInMillisConverterImplicits {

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
      case null => underlying.setObject(i, null)
      case AsIsParameterBinder(None) => bind(null, i)
      case AsIsParameterBinder(Some(value)) => bind(value, i)
      case AsIsParameterBinder(value) => bind(value, i)
      case binder: ParameterBinder => binder(underlying, i)
      case p: java.sql.Array => underlying.setArray(i, p)
      case p: BigDecimal => underlying.setBigDecimal(i, p.bigDecimal)
      case p: BigInt => underlying.setBigDecimal(i, new java.math.BigDecimal(p.bigInteger))
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
      case p if param.getClass.getCanonicalName.startsWith("java.time.") => {
        // Accessing JSR-310 APIs via Java reflection
        // because scalikejdbc-core should work on not only Java 8 but 6 & 7.
        import java.lang.reflect.Method
        val className: String = param.getClass.getCanonicalName
        val clazz: Class[_] = Class.forName(className)
        className match {
          case "java.time.ZonedDateTime" | "java.time.OffsetDateTime" =>
            val instant = clazz.getMethod("toInstant").invoke(p) // java.time.Instant
            val dateClazz: Class[_] = Class.forName("java.util.Date") // java.util.Date
            val fromMethod: Method = dateClazz.getMethod("from", Class.forName("java.time.Instant"))
            val dateValue = fromMethod.invoke(null, instant).asInstanceOf[java.util.Date]
            underlying.setTimestamp(i, dateValue.toSqlTimestamp)
          case "java.time.Instant" =>
            val millis = clazz.getMethod("toEpochMilli").invoke(p).asInstanceOf[java.lang.Long]
            underlying.setTimestamp(i, new java.util.Date(millis).toSqlTimestamp)
          case "java.time.LocalDateTime" =>
            underlying.setTimestamp(i, org.joda.time.LocalDateTime.parse(p.toString).toDate.toSqlTimestamp)
          case "java.time.LocalDate" =>
            underlying.setDate(i, org.joda.time.LocalDate.parse(p.toString).toDate.toSqlDate)
          case "java.time.LocalTime" =>
            underlying.setTime(i, org.joda.time.LocalTime.parse(p.toString).toSqlTime)
        }
      }
      case p: java.io.InputStream => underlying.setBinaryStream(i, p)
      case p => {
        log.debug("The parameter(" + p + ") is bound as an Object.")
        underlying.setObject(i, p)
      }
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
            case ParameterBinder(v) => normalize(v)
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
          case result: String =>
            GlobalSettings.loggingSQLAndTime.maxColumnSize.collect {
              case maxSize if result.size > maxSize =>
                "'" + result.take(maxSize) + "... (" + result.size + ")" + "'"
            }.getOrElse {
              "'" + result + "'"
            }
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
      val sql = trimSpaces(SQLTemplateParser.trimComments(template)
        .replaceAll("\r", " ")
        .replaceAll("\n", " ")
        .replaceAll("\t", " "))
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
      GlobalSettings.loggingSQLAndTime.maxBatchParamSize.collect {
        case maxSize if batchParamsList.size > maxSize =>
          batchParamsList.take(maxSize).map(params => singleSqlString(params)).mkString(";" + eol + "   ") + ";" + eol +
            "   ... (total: " + batchParamsList.size + " times)"
      }.getOrElse {
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
      GlobalSettings.taggedQueryCompletionListener(template, singleParams, spentMillis, tags)

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
        GlobalSettings.taggedQueryFailureListener.apply(template, singleParams, e, tags)

        throw e
    }
  }

  /**
   * Executes SQL statement
   */
  private[this] val statementExecute = new NakedExecutor with LoggingSQLAndTiming with LoggingSQLIfFailed

  def generatedKeysResultSet: java.sql.ResultSet = underlying.getGeneratedKeys

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
