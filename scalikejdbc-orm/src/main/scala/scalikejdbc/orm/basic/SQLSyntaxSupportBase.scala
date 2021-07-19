package scalikejdbc.orm.basic

// Don't change this import
import scalikejdbc._
import scalikejdbc.orm.{ Alias, JodaTimeImplicits }

/**
 * DataMapper base.
 *
 * @tparam Entity entity
 */
trait SQLSyntaxSupportBase[Entity]
  extends SQLSyntaxSupport[Entity]
  with ConnectionPoolFeature
  with JodaTimeImplicits {

  private[this] val _tableName = super.tableName
  private[this] val _columnNames = super.columnNames
  private[this] val _self: SQLSyntaxSupportBase[Entity] = this
  // NOTE: this field must be lazy one
  private[this] lazy val _defaultOrderings = defaultOrderings

  protected def underlying: SQLSyntaxSupportBase[Entity] =
    new SQLSyntaxSupportBase[Entity] {
      override def defaultAlias = _self.defaultAlias

      override val tableName = _tableName
      override val columnNames = _columnNames

      override def defaultOrderings = _defaultOrderings

      def extract(rs: WrappedResultSet, n: ResultName[Entity]) =
        _self.extract(rs, n)
    }

  /**
   * Returns default table alias.
   * This method is abstract and should be implemented with unique name.
   * override def defaultAlias = createAlias("sm")
   */
  def defaultAlias: Alias[Entity]

  /**
   * Returns select query builder object to simply fetch rows without other joined tables.
   */
  def singleSelectQuery: SelectSQLBuilder[Entity] =
    select.from(as(defaultAlias))

  /**
   * Returns simple select count query.
   */
  def simpleCountQuery: SelectSQLBuilder[Entity] =
    select(sqls.count).from(as(defaultAlias))

  /**
   * Returns select query builder.
   */
  def defaultSelectQuery: SelectSQLBuilder[Entity] = singleSelectQuery

  /**
   * Returns default scope for select queries.
   */
  def defaultScope(alias: Alias[Entity]): Option[SQLSyntax] = None

  def defaultScopeWithDefaultAlias: Option[SQLSyntax] = defaultScope(
    defaultAlias
  )

  def defaultOrderings: Seq[SQLSyntax]

  /**
   * Returns primary key name. (default: "id")
   * Notice: Not a column name but field name. For example, not "first_name" but "firstName"
   */
  def primaryKeyFieldName: String = "id"

  def primaryKeyField: SQLSyntax = defaultAlias.field(primaryKeyFieldName)

  /**
   * Returns table name.
   */
  override def tableName = underlying.tableName

  /**
   * Returns column names.
   */
  override def columnNames = underlying.columnNames

  /**
   * Creates a new table alias for this mapper.
   */
  def createAlias(name: String): Alias[Entity] = syntax(name)

  /**
   * Provides a code block with a table alias.
   */
  def withAlias[A](op: Alias[Entity] => A): A = op(defaultAlias)

  /**
   * Provides a code block with a table alias.
   */
  def withAlias[A](name: String)(op: Alias[Entity] => A): A = op(
    createAlias(name)
  )

  /**
   * Provides a code block with the column name provider.
   */
  def withColumns[A](op: ColumnName[Entity] => A): A = op(column)

  /**
   * Predicates the field name is valid.
   */
  def isValidFieldName(name: String): Boolean = {
    try Option(this.column.field(name)).isDefined
    catch {
      case e: InvalidColumnNameException => false
    }
  }

  /**
   * Extracts entity from ResultSet.
   */
  def apply(a: Alias[Entity])(rs: WrappedResultSet): Entity =
    extract(rs, a.resultName)

  /**
   * Extracts entity from ResultSet.
   */
  def apply(rs: WrappedResultSet): Entity = extract(rs, defaultAlias.resultName)

  /**
   * Extracts entity from ResultSet.
   */
  def extract(rs: WrappedResultSet, n: ResultName[Entity]): Entity

}
