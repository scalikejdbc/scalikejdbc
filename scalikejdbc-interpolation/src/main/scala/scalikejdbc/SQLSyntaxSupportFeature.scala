package scalikejdbc

import java.util.Locale.{ ENGLISH => en }
import scalikejdbc.interpolation.SQLSyntax

import scala.collection.concurrent.TrieMap
import scala.language.dynamics

/**
 * SQLSyntaxSupport feature
 */
object SQLSyntaxSupportFeature extends LogSupport {

  private val whiteSpaceRegExp = ".*\\s+.*".r
  private val semicolonRegExp = ".*;.*".r

  /**
   * Loaded columns for tables.
   */
  private[scalikejdbc] val SQLSyntaxSupportLoadedColumns
    : TrieMap[(Any, String), Seq[String]] = {
    new scala.collection.concurrent.TrieMap[(Any, String), Seq[String]]()
  }

  /**
   * Cached columns for columns providers.
   *
   * NOTE: Don't clear the Map value simply. We should clear only distal TrieMap[String, SQLSyntax] values.
   */
  private[scalikejdbc] val SQLSyntaxSupportCachedColumns
    : TrieMap[(Any, String), TrieMap[Any, TrieMap[String, SQLSyntax]]] = {
    new scala.collection.concurrent.TrieMap[(Any, String), TrieMap[
      Any,
      TrieMap[String, SQLSyntax]
    ]]()
  }

  /**
   * Instant table name validator.
   *
   * Notice: Table name is specified with a String value which might be an input value.
   */
  def verifyTableName(tableNameWithSchema: String): Unit = if (
    tableNameWithSchema != null
  ) {
    val name = tableNameWithSchema.trim
    val hasWhiteSpace = whiteSpaceRegExp.pattern.matcher(name).matches
    val hasSemicolon = semicolonRegExp.pattern.matcher(name).matches
    if (hasWhiteSpace || hasSemicolon) {
      log.warn(
        s"The table name (${name}) might bring you SQL injection vulnerability."
      )
    }
  }

}

/**
 * SQLSyntaxSupport feature
 */
trait SQLSyntaxSupportFeature { self: SQLInterpolationFeature =>

  object SQLSyntaxSupport {

    /**
     * Clears all the loaded column names.
     */
    def clearAllLoadedColumns(): Unit = {
      SQLSyntaxSupportFeature.SQLSyntaxSupportLoadedColumns.clear()

      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns.foreach {
        case (_, caches) =>
          caches.foreach { case (_, cache: TrieMap[String, SQLSyntax]) =>
            cache.clear()
          }
      }
    }

    /**
     * Clears all the loaded column names for specified connectionPoolName.
     */
    def clearLoadedColumns(
      connectionPoolName: Any = ConnectionPool.DEFAULT_NAME
    ): Unit = {
      val loadedColumns = SQLSyntaxSupportFeature.SQLSyntaxSupportLoadedColumns
      loadedColumns.keys
        .withFilter { case (cp, _) => cp == connectionPoolName }
        .foreach { case (cp, table) => loadedColumns.remove((cp, table)) }

      val cachedColumns = SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
      cachedColumns.keys
        .withFilter { case (cp, _) => cp == connectionPoolName }
        .foreach { case (cp, table) =>
          cachedColumns.get((cp, table)).foreach {
            _.foreach({ case (_, cache: TrieMap[String, SQLSyntax]) =>
              cache.clear()
            })
          }
        }
    }
  }

  /**
   * SQLSyntaxSupport trait. Companion object needs this trait as follows.
   *
   * {{{
   *   case class Member(id: Long, name: Option[String])
   *   object Member extends SQLSyntaxSupport[Member]
   * }}}
   */
  trait SQLSyntaxSupport[A] {

    protected[this] def settings: SettingsProvider =
      SettingsProvider.default

    /**
     * Connection Pool Name. If you use NamedDB, you must override this method.
     */
    def connectionPoolName: Any = ConnectionPool.DEFAULT_NAME

    /**
     * Auto session for current connection pool.
     */
    def autoSession: DBSession = NamedAutoSession(connectionPoolName)

    /**
     * Schema name if exists.
     */
    def schemaName: Option[String] = None

    /**
     * Table name (default: the snake_case name from this companion object's name).
     */
    def tableName: String = {
      val className = getClassSimpleName(this)
        .replaceFirst("\\$$", "")
        .replaceFirst("^.+\\.", "")
        .replaceFirst("^.+\\$", "")
      SQLSyntaxProvider.toColumnName(
        className,
        nameConverters,
        useSnakeCaseColumnName
      )
    }

    /**
     * Table name with schema name.
     */
    def tableNameWithSchema: String = schemaName
      .map { schema => s"${schema}.${tableName}" }
      .getOrElse(tableName)

    private[this] def getClassSimpleName(obj: Any): String = {
      try obj.getClass.getSimpleName
      catch {
        case e: InternalError =>
          // working on the Scala REPL
          val clazz = obj.getClass
          val classOfClazz = clazz.getClass
          val getSimpleBinaryName = classOfClazz.getDeclaredMethods
            .find(_.getName == "getSimpleBinaryName")
            .get
          getSimpleBinaryName.setAccessible(true)
          getSimpleBinaryName.invoke(clazz).toString
      }
    }

    /**
     * [[scalikejdbc.interpolation.SQLSyntax]] value for table name.
     *
     * Notice: Table name is specified with a String value which might be an input value.
     */
    def table: TableDefSQLSyntax = {
      SQLSyntaxSupportFeature.verifyTableName(tableNameWithSchema)
      TableDefSQLSyntax(tableNameWithSchema)
    }

    /**
     * Column names for this table (default: column names that are loaded from JDBC metadata).
     */
    def columns: collection.Seq[String] = {
      if (columnNames.isEmpty) {
        SQLSyntaxSupportFeature.SQLSyntaxSupportLoadedColumns.getOrElseUpdate(
          (connectionPoolName, tableNameWithSchema), {
            NamedDB(connectionPoolName, settings)
              .getColumnNames(tableNameWithSchema, tableTypes)
              .map(_.toLowerCase(en)) match {
              case Nil =>
                throw new IllegalStateException(
                  "No column found for " + tableName + ". If you use NamedDB, you must override connectionPoolName."
                )
              case cs => cs
            }
          }
        )
      } else columnNames
    }

    /**
     * Clears column names loaded from JDBC metadata.
     */
    def clearLoadedColumns(): Unit = {
      SQLSyntaxSupportFeature.SQLSyntaxSupportLoadedColumns.remove(
        (connectionPoolName, tableNameWithSchema)
      )

      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .find { case ((cp, tb), _) =>
          cp == connectionPoolName && tb == tableNameWithSchema
        }
        .foreach { case (_, caches) =>
          caches.foreach { case (_, cache: TrieMap[String, SQLSyntax]) =>
            cache.clear()
          }
        }
    }

    /**
     * If you prefer columnNames than columns, override this method to customize.
     */
    def columnNames: collection.Seq[String] = Nil

    /**
     * If you need some exotic table types like `MATERIALIZED VIEW` from PostgreSQL, override this method.
     */
    def tableTypes: Array[String] = DBConnection.tableTypes

    /**
     * True if you need forcing upper column names in SQL.
     */
    def forceUpperCase: Boolean = false

    /**
     * True if you need shortening alias names in SQL.
     */
    def useShortenedResultName: Boolean = true

    /**
     * True if you need to convert field names to snake_case column names in SQL.
     */
    def useSnakeCaseColumnName: Boolean = true

    /**
     * Delimiter for alias names in SQL.
     */
    def delimiterForResultName: String = if (forceUpperCase) "_ON_" else "_on_"

    /**
     * Rule to convert field names to column names.
     *
     * {{{
     *   override val nameConverters = Map("^serviceCode$" -> "service_cd")
     * }}}
     */
    def nameConverters: Map[String, String] = Map()

    /**
     * Returns ColumnName provider for this (expected to use for insert/update queries).
     */
    def column: ColumnName[A] =
      ColumnSQLSyntaxProvider[SQLSyntaxSupport[A], A](this)

    /**
     * Returns SQLSyntax provider for this.
     *
     * {{{
     *   val m = Member.syntax
     *   sql"select ${m.result.*} from ${Member as m}".map(Member(m.resultName)).list.apply()
     *   // select member.id as i_on_member, member.name as n_on_member from member
     * }}}
     */
    def syntax: QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A] = {
      val _tableName = tableNameWithSchema.replaceAll("\\.", "_")
      val _name = if (forceUpperCase) _tableName.toUpperCase(en) else _tableName
      QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A](this, _name)
    }

    /**
     * Returns SQLSyntax provider for this.
     *
     * {{{
     *   val m = Member.syntax("m")
     *   sql"select ${m.result.*} from ${Member as m}".map(Member(m.resultName)).list.apply()
     *   // select m.id as i_on_m, m.name as n_on_m from member m
     * }}}
     */
    def syntax(name: String): QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A] = {
      val _name = if (forceUpperCase) name.toUpperCase(en) else name
      QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A](this, _name)
    }

    /**
     * Returns table name and alias name part in SQL. If alias name and table name are same, alias name will be skipped.
     *
     * {{{
     *   sql"select ${m.result.*} from ${Member.as(m)}"
     * }}}
     */
    def as(
      provider: QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A]
    ): TableAsAliasSQLSyntax = {
      if (tableName == provider.tableAliasName) {
        TableAsAliasSQLSyntax(table.value, table.rawParameters, Some(provider))
      } else {
        TableAsAliasSQLSyntax(
          tableNameWithSchema + " " + provider.tableAliasName,
          Nil,
          Some(provider)
        )
      }
    }
  }

  /**
   * Table definition (which has alias name) part SQLSyntax
   */
  case class TableAsAliasSQLSyntax private[scalikejdbc] (
    override val value: String,
    override val rawParameters: collection.Seq[Any] = Vector(),
    resultAllProvider: Option[ResultAllProvider] = None
  ) extends SQLSyntax(value, rawParameters)

  /**
   * Table definition part SQLSyntax
   */
  case class TableDefSQLSyntax private[scalikejdbc] (
    override val value: String,
    override val rawParameters: collection.Seq[Any] = Vector()
  ) extends SQLSyntax(value, rawParameters)

  /**
   * SQLSyntax Provider
   */
  trait SQLSyntaxProvider[A] extends Dynamic with SelectDynamicMacro[A] {
    import SQLSyntaxProvider._

    /**
     * Rule to convert field names to column names.
     */
    val nameConverters: Map[String, String]

    /**
     * True if you need forcing upper column names in SQL.
     */
    val forceUpperCase: Boolean

    /**
     * Delimiter for alias names in SQL.
     */
    def delimiterForResultName: String

    /**
     * True if you need to convert field names to snake_case column names in SQL.
     */
    val useSnakeCaseColumnName: Boolean

    /**
     * Returns [[scalikejdbc.interpolation.SQLSyntax]] value for the column.
     */
    def c(name: String): SQLSyntax = column(name)

    /**
     * Returns [[scalikejdbc.interpolation.SQLSyntax]] value for the column.
     */
    def column(name: String): SQLSyntax

    /**
     * Returns [[scalikejdbc.interpolation.SQLSyntax]] value for the column which is referred by the field.
     */
    def field(name: String): scalikejdbc.interpolation.SQLSyntax = {
      val columnName = {
        if (forceUpperCase)
          toColumnName(name, nameConverters, useSnakeCaseColumnName)
            .toUpperCase(en)
        else toColumnName(name, nameConverters, useSnakeCaseColumnName)
      }
      c(columnName)
    }

  }

  /**
   * SQLSyntaxProvider companion.
   */
  private[scalikejdbc] object SQLSyntaxProvider {

    private val acronymRegExpStr = "[A-Z]{2,}"
    private val acronymRegExp = acronymRegExpStr.r
    private val endsWithAcronymRegExp = "[A-Z]{2,}$".r
    private val singleUpperCaseRegExp = """[A-Z]""".r
    private val danglingUnderscoreRegExp = "^_|_$".r
    private val underscoreRegExp = "_".r

    /**
     * Returns the snake_case name after applying nameConverters.
     */
    def toColumnName(
      str: String,
      nameConverters: Map[String, String],
      useSnakeCaseColumnName: Boolean
    ): String = {
      val convertersApplied = {
        if (nameConverters != null) nameConverters.foldLeft(str) {
          case (s, (from, to)) => s.replaceAll(from, to)
        }
        else str
      }
      if (useSnakeCaseColumnName) {
        val acronymsFiltered = acronymRegExp.replaceAllIn(
          endsWithAcronymRegExp
            .replaceAllIn(
              convertersApplied,
              { m => "_" + m.matched.toLowerCase(en) }
            ), // might end with an acronym
          { m =>
            "_" + m.matched.init.toLowerCase(en) + "_" + m.matched.last.toString
              .toLowerCase(en)
          }
        )
        val mergedAcronyms = singleUpperCaseRegExp
          .replaceAllIn(
            acronymsFiltered,
            { m => "_" + m.matched.toLowerCase(en) }
          )

        val result = danglingUnderscoreRegExp.replaceAllIn(mergedAcronyms, "")

        if (str.startsWith("_")) "_" + result
        else if (str.endsWith("_")) result + "_"
        else result

      } else {
        convertersApplied
      }
    }

    /**
     * Returns the shortened name for the name.
     */
    def toShortenedName(
      name: String,
      columns: collection.Seq[String]
    ): String = {
      def shorten(s: String): String =
        underscoreRegExp.split(s).map(_.take(1)).mkString

      val shortenedName = shorten(toAlphabetOnly(name))
      val shortenedNames = columns.map(c => shorten(toAlphabetOnly(c)))
      if (shortenedNames.count(_ == shortenedName) > 1) {
        val (n, found) = columns.zip(shortenedNames).foldLeft((1, false)) {
          case ((n, found), (column, shortened)) =>
            if (found) {
              (n, found) // already found
            } else if (column == name) {
              (n, true) // original name is expected
            } else if (shortened == shortenedName) {
              (
                n + 1,
                false
              ) // original name is different but shorten name is same
            } else {
              (n, found) // not found yet
            }
        }
        if (!found)
          throw new IllegalStateException("This must be a library bug.")
        else shortenedName + n
      } else {
        shortenedName
      }
    }

    /**
     * Returns the alias name for the name.
     */
    def toAliasName(
      originalName: String,
      support: SQLSyntaxSupport[?]
    ): String = {
      if (support.useShortenedResultName)
        toShortenedName(originalName, support.columns)
      else originalName
    }

    /**
     * Returns the name which is converted to pure alphabet only.
     */
    private[this] def toAlphabetOnly(name: String): String = {
      val _name = name.filter(c => c.isLetter && c <= 'z' || c == '_')
      if (_name.isEmpty) "x" else _name
    }

  }

  /**
   * SQLSyntax provider for column names.
   */
  case class ColumnSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S)
    extends SQLSyntaxProvider[A]
    with AsteriskProvider {

    val nameConverters: Map[String, String] = support.nameConverters
    val forceUpperCase: Boolean = support.forceUpperCase
    val useSnakeCaseColumnName: Boolean = support.useSnakeCaseColumnName

    lazy val delimiterForResultName: String =
      throw new UnsupportedOperationException(
        "It's a library bug if this exception is thrown."
      )

    lazy val columns: collection.Seq[SQLSyntax] = support.columns
      .map { c => if (support.forceUpperCase) c.toUpperCase(en) else c }
      .map(c => SQLSyntax(c))

    lazy val * : SQLSyntax = SQLSyntax(columns.map(_.value).mkString(", "))

    val asterisk: SQLSyntax = sqls"*"

    private[this] lazy val cachedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (support.connectionPoolName, support.tableNameWithSchema),
          TrieMap.empty
        )
        .put(this, cc)
      cc
    }

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        columns
          .find(_.value.equalsIgnoreCase(name))
          .map { c =>
            SQLSyntax(c.value)
          }
          .getOrElse {
            throw new InvalidColumnNameException(
              ErrorMessage.INVALID_COLUMN_NAME +
                s" (name: ${name}, registered names: ${columns.map(_.value).mkString(",")})"
            )
          }
      }
    )

  }

  /**
   * SQLSyntax Provider basic implementation.
   */
  private[scalikejdbc] abstract class SQLSyntaxProviderCommonImpl[
    S <: SQLSyntaxSupport[A],
    A
  ](support: S, tableAliasName: String)
    extends SQLSyntaxProvider[A] {

    val nameConverters: Map[String, String] = support.nameConverters
    val forceUpperCase: Boolean = support.forceUpperCase
    val useSnakeCaseColumnName: Boolean = support.useSnakeCaseColumnName
    val delimiterForResultName: String = support.delimiterForResultName
    lazy val columns: collection.Seq[SQLSyntax] = support.columns
      .map { c => if (support.forceUpperCase) c.toUpperCase(en) else c }
      .map(c => SQLSyntax(c))

    def notFoundInColumns(
      aliasName: String,
      name: String
    ): InvalidColumnNameException =
      notFoundInColumns(aliasName, name, columns.map(_.value).mkString(","))

    def notFoundInColumns(
      aliasName: String,
      name: String,
      registeredNames: String
    ): InvalidColumnNameException = {
      new InvalidColumnNameException(
        ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${aliasName}.${name}, registered names: ${registeredNames})"
      )
    }

  }

  /**
   * SQLSyntax provider for query parts.
   */
  case class QuerySQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    support: S,
    tableAliasName: String
  ) extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName)
    with ResultAllProvider
    with AsteriskProvider {

    val result: ResultSQLSyntaxProvider[S, A] = {
      val table =
        if (support.forceUpperCase) tableAliasName.toUpperCase(en)
        else tableAliasName
      ResultSQLSyntaxProvider[S, A](support, table)
    }

    override def resultAll: SQLSyntax = result.*

    val resultName: BasicResultNameSQLSyntaxProvider[S, A] = result.nameProvider

    lazy val * : SQLSyntax = SQLSyntax(
      columns.map(c => s"${tableAliasName}.${c.value}").mkString(", ")
    )

    val asterisk: SQLSyntax = SQLSyntax(tableAliasName + ".*")

    private[this] lazy val cachedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (support.connectionPoolName, support.tableNameWithSchema),
          TrieMap.empty
        )
        .put(this, cc)
      cc
    }

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        columns
          .find(_.value.equalsIgnoreCase(name))
          .map { c =>
            SQLSyntax(s"${tableAliasName}.${c.value}")
          }
          .getOrElse {
            throw new InvalidColumnNameException(
              ErrorMessage.INVALID_COLUMN_NAME +
                s" (name: ${tableAliasName}.${name}, registered names: ${columns.map(_.value).mkString(",")})"
            )
          }
      }
    )

  }

  /**
   * SQLSyntax provider for result parts.
   */
  case class ResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    support: S,
    tableAliasName: String
  ) extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {
    import SQLSyntaxProvider._

    private[scalikejdbc] val nameProvider
      : BasicResultNameSQLSyntaxProvider[S, A] =
      BasicResultNameSQLSyntaxProvider[S, A](support, tableAliasName)

    lazy val * : SQLSyntax = SQLSyntax(
      columns
        .map { c =>
          val name = toAliasName(c.value, support)
          s"${tableAliasName}.${c.value} as ${name}${delimiterForResultName}${tableAliasName}"
        }
        .mkString(", ")
    )

    def apply(syntax: SQLSyntax): PartialResultSQLSyntaxProvider[S, A] =
      PartialResultSQLSyntaxProvider(support, tableAliasName, syntax)

    private[this] lazy val cachedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (support.connectionPoolName, support.tableNameWithSchema),
          TrieMap.empty
        )
        .put(this, cc)
      cc
    }

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        columns
          .find(_.value.equalsIgnoreCase(name))
          .map { c =>
            val name = toAliasName(c.value, support)
            SQLSyntax(
              s"${tableAliasName}.${c.value} as ${name}${delimiterForResultName}${tableAliasName}"
            )
          }
          .getOrElse(throw notFoundInColumns(tableAliasName, name))
      }
    )
  }

  case class PartialResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    support: S,
    aliasName: String,
    syntax: SQLSyntax
  ) extends SQLSyntaxProviderCommonImpl[S, A](support, aliasName) {
    import SQLSyntaxProvider._

    private[this] lazy val cachedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (support.connectionPoolName, support.tableNameWithSchema),
          TrieMap.empty
        )
        .put(this, cc)
      cc
    }

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        columns
          .find(_.value.equalsIgnoreCase(name))
          .map { c =>
            val name = toAliasName(c.value, support)
            SQLSyntax(
              s"${syntax.value} as ${name}${delimiterForResultName}${aliasName}",
              syntax.rawParameters
            )
          }
          .getOrElse(throw notFoundInColumns(aliasName, name))
      }
    )
  }

  /**
   * SQLSyntax provider for result.nameProviders.
   */
  trait ResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A]
    extends SQLSyntaxProvider[A] {
    def * : SQLSyntax
    def namedColumns: collection.Seq[SQLSyntax]
    def namedColumn(name: String): SQLSyntax
    def column(name: String): SQLSyntax
  }

  /**
   * Basic Query SQLSyntax Provider for result.nameProviders.
   */
  case class BasicResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    support: S,
    tableAliasName: String
  ) extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName)
    with ResultNameSQLSyntaxProvider[S, A] {

    import SQLSyntaxProvider._

    lazy val * : SQLSyntax = SQLSyntax(
      columns
        .map { c =>
          val name = toAliasName(c.value, support)
          s"${name}${delimiterForResultName}${tableAliasName}"
        }
        .mkString(", ")
    )

    lazy val namedColumns: collection.Seq[SQLSyntax] = support.columns.map {
      (columnName: String) =>
        val name = toAliasName(columnName, support)
        SQLSyntax(s"${name}${delimiterForResultName}${tableAliasName}")
    }

    private[this] lazy val cachedNamedColumns =
      new scala.collection.concurrent.TrieMap[String, SQLSyntax]

    def namedColumn(name: String): SQLSyntax =
      cachedNamedColumns.getOrElseUpdate(
        name, {
          namedColumns.find(_.value.equalsIgnoreCase(name)).getOrElse {
            throw new InvalidColumnNameException(
              ErrorMessage.INVALID_COLUMN_NAME +
                s" (name: ${name}, registered names: ${namedColumns.map(_.value).mkString(",")})"
            )
          }
        }
      )

    private[this] lazy val cachedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (support.connectionPoolName, support.tableNameWithSchema),
          TrieMap.empty
        )
        .put(this, cc)
      cc
    }

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        columns
          .find(_.value.equalsIgnoreCase(name))
          .map { c =>
            val name = toAliasName(c.value, support)
            SQLSyntax(s"${name}${delimiterForResultName}${tableAliasName}")
          }
          .getOrElse(throw notFoundInColumns(tableAliasName, name))
      }
    )

    private[this] val cachedFields =
      new scala.collection.concurrent.TrieMap[String, SQLSyntax]

    override def field(name: String): SQLSyntax =
      cachedFields.getOrElseUpdate(name, super.field(name))
  }

  // subquery syntax providers

  object SubQuery {

    def syntax(
      name: String,
      resultNames: BasicResultNameSQLSyntaxProvider[?, ?]*
    ): SubQuerySQLSyntaxProvider = {
      SubQuerySQLSyntaxProvider(
        name,
        resultNames.head.delimiterForResultName,
        resultNames
      )
    }

    def syntax(
      name: String,
      delimiterForResultName: String,
      resultNames: BasicResultNameSQLSyntaxProvider[?, ?]*
    ): SubQuerySQLSyntaxProvider = {
      SubQuerySQLSyntaxProvider(name, delimiterForResultName, resultNames)
    }

    def syntax(name: String): SubQuerySQLSyntaxProviderBuilder =
      SubQuerySQLSyntaxProviderBuilder(name)

    def syntax(
      name: String,
      delimiterForResultName: String
    ): SubQuerySQLSyntaxProviderBuilder = {
      SubQuerySQLSyntaxProviderBuilder(name, Option(delimiterForResultName))
    }

    case class SubQuerySQLSyntaxProviderBuilder(
      name: String,
      delimiterForResultName: Option[String] = None
    ) {
      def include(
        syntaxProviders: QuerySQLSyntaxProvider[?, ?]*
      ): SubQuerySQLSyntaxProvider = {
        SubQuery.syntax(
          name,
          delimiterForResultName
            .orElse(
              syntaxProviders.headOption
                .map(_.resultName.delimiterForResultName)
            )
            .getOrElse("_on_"),
          syntaxProviders.map(_.resultName)*
        )
      }
    }

    def as(subquery: SubQuerySQLSyntaxProvider): TableDefSQLSyntax =
      TableDefSQLSyntax(subquery.aliasName)

  }

  case class SubQuerySQLSyntaxProvider(
    aliasName: String,
    delimiterForResultName: String,
    resultNames: collection.Seq[BasicResultNameSQLSyntaxProvider[?, ?]]
  ) extends ResultAllProvider
    with AsteriskProvider {

    val result: SubQueryResultSQLSyntaxProvider =
      SubQueryResultSQLSyntaxProvider(
        aliasName,
        delimiterForResultName,
        resultNames
      )
    val resultName: SubQueryResultNameSQLSyntaxProvider = result.nameProvider

    override def resultAll: SQLSyntax = result.*

    lazy val * : SQLSyntax = SQLSyntax(
      resultNames
        .map { resultName =>
          resultName.namedColumns
            .map { c =>
              s"${aliasName}.${c.value}"
            }
            .mkString(", ")
        }
        .mkString(", ")
    )

    val asterisk: SQLSyntax = SQLSyntax(aliasName + ".*")

    def apply(name: SQLSyntax): SQLSyntax = {
      val foundResultName: Option[SQLSyntax] = {
        resultNames
          .find(rn =>
            rn.namedColumns.exists(_.value.equalsIgnoreCase(name.value))
          )
          .map { rn =>
            SQLSyntax(s"${aliasName}.${rn.namedColumn(name.value).value}")
          }
      }
      foundResultName.getOrElse {
        val registeredNames = resultNames
          .map { rn => rn.namedColumns.map(_.value).mkString(",") }
          .mkString(",")
        throw new InvalidColumnNameException(
          ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name.value}, registered names: ${registeredNames})"
        )
      }
    }

    def apply[S <: SQLSyntaxSupport[A], A](
      syntax: QuerySQLSyntaxProvider[S, A]
    ): PartialSubQuerySQLSyntaxProvider[S, A] = {
      PartialSubQuerySQLSyntaxProvider(
        aliasName,
        delimiterForResultName,
        syntax.resultName
      )
    }

  }

  case class SubQueryResultSQLSyntaxProvider(
    aliasName: String,
    delimiterForResultName: String,
    resultNames: collection.Seq[BasicResultNameSQLSyntaxProvider[?, ?]]
  ) {

    private[scalikejdbc] val nameProvider: SubQueryResultNameSQLSyntaxProvider =
      SubQueryResultNameSQLSyntaxProvider(
        aliasName,
        delimiterForResultName,
        resultNames
      )

    def * : SQLSyntax = SQLSyntax(
      resultNames
        .map { rn =>
          rn.namedColumns
            .map { c =>
              s"${aliasName}.${c.value} as ${c.value}${delimiterForResultName}${aliasName}"
            }
            .mkString(", ")
        }
        .mkString(", ")
    )

    private[this] lazy val cachedColumns =
      new scala.collection.concurrent.TrieMap[String, SQLSyntax]

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        resultNames
          .find(rn => rn.namedColumns.exists(_.value.equalsIgnoreCase(name)))
          .map { rn =>
            SQLSyntax(s"${aliasName}.${rn.namedColumn(name).value} as ${rn
                .namedColumn(name)
                .value}${delimiterForResultName}${aliasName}")
          }
          .getOrElse {
            val registeredNames = resultNames
              .map { rn => rn.namedColumns.map(_.value).mkString(",") }
              .mkString(",")
            throw new InvalidColumnNameException(
              ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name}, registered names: ${registeredNames})"
            )
          }
      }
    )

  }

  case class SubQueryResultNameSQLSyntaxProvider(
    aliasName: String,
    delimiterForResultName: String,
    resultNames: collection.Seq[BasicResultNameSQLSyntaxProvider[?, ?]]
  ) {

    lazy val * : SQLSyntax = SQLSyntax(
      resultNames
        .map { rn =>
          rn.namedColumns
            .map { c =>
              s"${c.value}${delimiterForResultName}${aliasName}"
            }
            .mkString(", ")
        }
        .mkString(", ")
    )

    lazy val columns: collection.Seq[SQLSyntax] = resultNames.flatMap { rn =>
      rn.namedColumns.map { c =>
        SQLSyntax(s"${c.value}${delimiterForResultName}${aliasName}")
      }
    }

    private[this] lazy val cachedColumns =
      new scala.collection.concurrent.TrieMap[String, SQLSyntax]

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        columns.find(_.value.equalsIgnoreCase(name)).getOrElse {
          throw notFoundInColumns(aliasName, name)
        }
      }
    )

    def apply(name: SQLSyntax): SQLSyntax = {
      resultNames
        .find(rn =>
          rn.namedColumns.exists(_.value.equalsIgnoreCase(name.value))
        )
        .map { rn =>
          SQLSyntax(
            s"${rn.namedColumn(name.value).value}${delimiterForResultName}${aliasName}"
          )
        }
        .getOrElse {
          throw notFoundInColumns(aliasName, name.value)
        }
    }

    def notFoundInColumns(
      aliasName: String,
      name: String
    ): InvalidColumnNameException = {
      val registeredNames = resultNames
        .map { rn => rn.namedColumns.map(_.value).mkString(",") }
        .mkString(",")
      new InvalidColumnNameException(
        ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${aliasName}.${name}, registered names: ${registeredNames})"
      )
    }

  }

  // -----
  // partial subquery syntax providers

  case class PartialSubQuerySQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A]
  ) extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName)
    with AsteriskProvider {

    val result: PartialSubQueryResultSQLSyntaxProvider[S, A] = {
      PartialSubQueryResultSQLSyntaxProvider(
        aliasName,
        delimiterForResultName,
        underlying
      )
    }

    val resultName: PartialSubQueryResultNameSQLSyntaxProvider[S, A] =
      result.nameProvider

    lazy val * : SQLSyntax = SQLSyntax(
      resultName.namedColumns
        .map { c => s"${aliasName}.${c.value}" }
        .mkString(", ")
    )

    val asterisk: SQLSyntax = SQLSyntax(aliasName + ".*")

    def apply(name: SQLSyntax): SQLSyntax = {
      underlying.namedColumns
        .find(_.value.equalsIgnoreCase(name.value))
        .map { _ =>
          SQLSyntax(s"${aliasName}.${underlying.namedColumn(name.value).value}")
        }
        .getOrElse {
          throw notFoundInColumns(
            aliasName,
            name.value,
            resultName.columns.map(_.value).mkString(",")
          )
        }
    }

    private[this] lazy val cachedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (
            underlying.support.connectionPoolName,
            underlying.support.tableNameWithSchema
          ),
          TrieMap.empty
        )
        .put(this, cc)
      cc
    }

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        SQLSyntax(s"${aliasName}.${underlying.column(name).value}")
      }
    )

  }

  case class PartialSubQueryResultSQLSyntaxProvider[
    S <: SQLSyntaxSupport[A],
    A
  ](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A]
  ) extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) {

    private[scalikejdbc] val nameProvider
      : PartialSubQueryResultNameSQLSyntaxProvider[S, A] = {
      PartialSubQueryResultNameSQLSyntaxProvider(
        aliasName,
        delimiterForResultName,
        underlying
      )
    }

    lazy val * : SQLSyntax = SQLSyntax(
      underlying.namedColumns
        .map { c =>
          s"${aliasName}.${c.value} as ${c.value}${delimiterForResultName}${aliasName}"
        }
        .mkString(", ")
    )

    private[this] lazy val cachedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (
            underlying.support.connectionPoolName,
            underlying.support.tableNameWithSchema
          ),
          TrieMap.empty
        )
        .put(this, cc)
      cc
    }

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        underlying.columns
          .find(_.value.equalsIgnoreCase(name))
          .map { original =>
            val underlyingResultName = underlying.column(original.value).value
            SQLSyntax(
              s"${aliasName}.${underlyingResultName} as ${underlyingResultName}${delimiterForResultName}${aliasName}"
            )
          }
          .getOrElse {
            throw notFoundInColumns(
              aliasName,
              name,
              underlying.columns.map(_.value).mkString(",")
            )
          }
      }
    )

  }

  case class PartialSubQueryResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[
    A
  ], A](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A]
  ) extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName)
    with ResultNameSQLSyntaxProvider[S, A] {
    import SQLSyntaxProvider._

    lazy val * : SQLSyntax = SQLSyntax(
      underlying.namedColumns
        .map { c =>
          s"${c.value}${delimiterForResultName}${aliasName}"
        }
        .mkString(", ")
    )

    override lazy val columns: collection.Seq[SQLSyntax] =
      underlying.namedColumns.map { c =>
        SQLSyntax(s"${c.value}${delimiterForResultName}${aliasName}")
      }

    private[this] lazy val cachedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (
            underlying.support.connectionPoolName,
            underlying.support.tableNameWithSchema
          ),
          TrieMap.empty
        )
        .put(this.toString + "_cachedColumns", cc)
      cc
    }

    def column(name: String): SQLSyntax = cachedColumns.getOrElseUpdate(
      name, {
        underlying.columns
          .find(_.value.equalsIgnoreCase(name))
          .map { (original: SQLSyntax) =>
            val name = toAliasName(original.value, underlying.support)
            SQLSyntax(
              s"${name}${delimiterForResultName}${underlying.tableAliasName}${delimiterForResultName}${aliasName}"
            )
          }
          .getOrElse {
            throw notFoundInColumns(
              aliasName,
              name,
              underlying.columns.map(_.value).mkString(",")
            )
          }
      }
    )

    lazy val namedColumns: collection.Seq[SQLSyntax] =
      underlying.namedColumns.map { (nc: SQLSyntax) =>
        SQLSyntax(s"${nc.value}${delimiterForResultName}${aliasName}")
      }

    private[this] lazy val cachedNamedColumns = {
      val cc = new scala.collection.concurrent.TrieMap[String, SQLSyntax]
      SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
        .getOrElseUpdate(
          (
            underlying.support.connectionPoolName,
            underlying.support.tableNameWithSchema
          ),
          TrieMap.empty
        )
        .put(this.toString + "_cachedNamedColumns", cc)
      cc
    }

    def namedColumn(name: String): SQLSyntax =
      cachedNamedColumns.getOrElseUpdate(
        name, {
          underlying.namedColumns
            .find(_.value.equalsIgnoreCase(name))
            .getOrElse {
              throw notFoundInColumns(
                aliasName,
                name,
                namedColumns.map(_.value).mkString(",")
              )
            }
        }
      )

    def apply(name: SQLSyntax): SQLSyntax = {
      underlying.namedColumns
        .find(_.value.equalsIgnoreCase(name.value))
        .map { nc =>
          SQLSyntax(s"${nc.value}${delimiterForResultName}${aliasName}")
        }
        .getOrElse {
          throw notFoundInColumns(
            aliasName,
            name.value,
            underlying.columns.map(_.value).mkString(",")
          )
        }
    }

  }

  // ---------------------------------
  // Type aliases for this trait elements
  // ---------------------------------

  type ColumnName[A] = ColumnSQLSyntaxProvider[SQLSyntaxSupport[A], A]
  type ResultName[A] = ResultNameSQLSyntaxProvider[SQLSyntaxSupport[A], A]
  type SubQueryResultName = SubQueryResultNameSQLSyntaxProvider
  type SyntaxProvider[A] = QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A]
  type SubQuerySyntaxProvider = SubQuerySQLSyntaxProvider

}
