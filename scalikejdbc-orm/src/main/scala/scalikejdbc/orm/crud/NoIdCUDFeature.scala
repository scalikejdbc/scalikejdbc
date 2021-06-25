package scalikejdbc.orm.crud

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.strongparameters.{
  PermittedStrongParameters,
  StrongParametersFeature
}
import scalikejdbc.orm.basic.{
  AutoSessionFeature,
  ConnectionPoolFeature,
  SQLSyntaxSupportBase
}

import scala.collection.mutable

trait NoIdCUDFeature[Entity]
  extends SQLSyntaxSupportBase[Entity]
  with ConnectionPoolFeature
  with AutoSessionFeature
  with StrongParametersFeature {

  // -------------
  // Create
  // -------------

  /**
   * Attributes to be inserted when creation.
   */
  private[this] val attributesForCreation =
    new mutable.LinkedHashSet[(SQLSyntax, Any)]()

  /**
   * Accepted factories for attributesForCreation.
   */
  private[this] val attributesForCreationFactories =
    new mutable.LinkedHashSet[() => Boolean]()

  /**
   * Adds new attribute to be inserted when creation.
   *
   * @param namedValue named value
   * @return self
   */
  protected def addAttributeForCreation(
    namedValue: => (SQLSyntax, Any)
  ): this.type = {
    acceptAttributeForCreation(namedValue)
    this
  }

  /**
   * Attributes for creation are ready if true
   */
  private[this] lazy val attributesForCreationReady: Boolean = {
    attributesForCreationFactories.foreach(_.apply())
    true
  }

  /**
   * Accepts an attribute for creation.
   *
   * @param namedValue named value
   */
  private[this] def acceptAttributeForCreation(
    namedValue: => (SQLSyntax, Any)
  ): Unit = {
    attributesForCreationFactories.add(() =>
      attributesForCreation.add(namedValue)
    )
  }

  /**
   * Merges already registered attributes to be inserted and parameters.
   *
   * @param namedValues named values
   * @return merged attributes
   */
  protected def mergeNamedValuesForCreation(
    namedValues: Seq[(SQLSyntax, Any)]
  ): Seq[(SQLSyntax, Any)] = {
    if (!attributesForCreationReady) {
      throw new IllegalStateException(
        "Attributes for creation query is not ready!"
      )
    }

    namedValues.foldLeft(attributesForCreation) {
      case (xs, (column, newValue)) =>
        if (xs.exists(_._1 == column)) xs.map { case (c, v) =>
          if (c == column) (column, newValue) else (c, v)
        }
        else {
          val kv = (column, newValue)
          xs + kv
        }
    }
    val toBeInserted = attributesForCreation.++(namedValues).toSeq
    toBeInserted
  }

  /**
   * Extracts named values from the permitted parameters.
   *
   * @param strongParameters permitted parameters
   * @return named values
   */
  protected def namedValuesForCreation(
    strongParameters: PermittedStrongParameters
  ): Seq[(SQLSyntax, Any)] = {
    mergeNamedValuesForCreation(
      strongParameters.params.map { case (name, (value, paramType)) =>
        (
          column.field(name),
          getTypedValueFromStrongParameter(name, value, paramType)
        )
      }.toSeq
    )
  }

  def createWithNamedValues(
    namesAndValues: (SQLSyntax, Any)*
  )(implicit s: DBSession = autoSession): Any = {
    withSQL {
      insert
        .into(this)
        .namedValues(namesAndValues.map { case (k, v) =>
          k -> AsIsParameterBinder(v)
        }*)
    }.update.apply()
  }

  /**
   * Creates a new entity with permitted strong parameters.
   *
   * @param strongParameters permitted parameters
   * @param s                db session
   * @return created count (actually useless)
   */
  def createWithPermittedAttributes(
    strongParameters: PermittedStrongParameters
  )(implicit s: DBSession = autoSession): Any = {
    createWithNamedValues(namedValuesForCreation(strongParameters)*)
  }

  /**
   * Creates a new entity with non-permitted parameters.
   *
   * CAUTION: If you use this method in some web apps, you might have mass assignment vulnerability.
   *
   * @param parameters parameters
   * @param s          db session
   * @return created count (actually useless)
   */
  def createWithAttributes(
    parameters: (String, Any)*
  )(implicit s: DBSession = autoSession): Any = {
    createWithNamedValues(mergeNamedValuesForCreation(parameters.map {
      case (name, value) => (column.field(name), value)
    })*)
  }

  // -------------
  // Update
  // -------------

  /**
   * Returns default scope for update/delete operations.
   *
   * @return default scope
   */
  def defaultScopeForUpdateOperations: Option[SQLSyntax] = None

  /**
   * Returns update query builder with condition.
   *
   * @param where where condition
   * @return update query builder
   */
  def updateBy(where: SQLSyntax): UpdateOperationBuilder = {
    new UpdateOperationBuilder(
      this,
      where,
      beforeUpdateByHandlers.toSeq,
      afterUpdateByHandlers.toSeq
    )
  }

  /**
   * #updateBy pre-execution handler.
   */
  type BeforeUpdateByHandler =
    (DBSession, SQLSyntax, Seq[(SQLSyntax, Any)]) => Unit

  /**
   * #updateBy post-execution handler.
   */
  type AfterUpdateByHandler =
    (DBSession, SQLSyntax, Seq[(SQLSyntax, Any)], Int) => Unit

  /**
   * Registered beforeUpdateByHandlers.
   */
  protected val beforeUpdateByHandlers =
    new scala.collection.mutable.ListBuffer[BeforeUpdateByHandler]

  /**
   * Registered afterUpdateByHandlers.
   */
  protected val afterUpdateByHandlers =
    new scala.collection.mutable.ListBuffer[AfterUpdateByHandler]

  /**
   * Registers #updateBy pre-execution handler.
   *
   * @param handler event handler
   */
  protected def beforeUpdateBy(handler: BeforeUpdateByHandler): Unit =
    beforeUpdateByHandlers.append(handler)

  /**
   * Registers #updateBy post-execution handler.
   *
   * @param handler event handler
   */
  protected def afterUpdateBy(handler: AfterUpdateByHandler): Unit =
    afterUpdateByHandlers.append(handler)

  /**
   * Update query builder/executor.
   *
   * @param mapper mapper
   * @param where  condition
   */
  class UpdateOperationBuilder(
    mapper: NoIdCUDFeature[Entity],
    where: SQLSyntax,
    beforeHandlers: Seq[BeforeUpdateByHandler],
    afterHandlers: Seq[AfterUpdateByHandler]
  ) {

    /**
     * Attributes to be updated.
     */
    private[this] val attributesToBeUpdated =
      new mutable.HashMap[SQLSyntax, Any]()

    /**
     * Additional query parts after `set` keyword.
     */
    private[this] val additionalUpdateSQLs =
      new mutable.LinkedHashSet[SQLSyntax]()

    /**
     * Adds new attribute to be updated.
     *
     * @param namedValue named value
     * @return self
     */
    def addAttributeToBeUpdated(
      namedValue: (SQLSyntax, Any)
    ): UpdateOperationBuilder = {
      attributesToBeUpdated.update(namedValue._1, namedValue._2)
      this
    }

    /**
     * Adds new query part.
     *
     * @param queryPart query part
     * @return self
     */
    def addUpdateSQLPart(queryPart: SQLSyntax): UpdateOperationBuilder = {
      additionalUpdateSQLs.add(queryPart)
      this
    }

    /**
     * Converts permitted strong parameters to named values.
     *
     * @param strongParameters permitted parameters
     * @return named values
     */
    protected def toNamedValuesToBeUpdated(
      strongParameters: PermittedStrongParameters
    ): Seq[(SQLSyntax, Any)] = {
      strongParameters.params.map { case (name, (value, paramType)) =>
        (
          column.field(name),
          getTypedValueFromStrongParameter(name, value, paramType)
        )
      }.toSeq
    }

    /**
     * Merges already registered attributes to be updated and parameters.
     *
     * @param namedValues named values
     * @return merged attributes
     */
    protected def mergeNamedValues(
      namedValues: Seq[(SQLSyntax, Any)]
    ): Seq[(SQLSyntax, Any)] = {
      namedValues
        .foldLeft(attributesToBeUpdated) { case (xs, (column, newValue)) =>
          if (xs.exists(_._1 == column)) xs.map { case (c, v) =>
            if (c == column) (column, newValue) else (c, v)
          }
          else {
            val kv = (column, newValue)
            xs += kv
          }
        }
        .toSeq
    }

    /**
     * Merges additional query parts.
     *
     * @param queryBuilder   query builder
     * @param othersAreEmpty other attributes to be updated is empty if true
     * @return query builder
     */
    protected def mergeAdditionalUpdateSQLs(
      queryBuilder: UpdateSQLBuilder,
      othersAreEmpty: Boolean
    ): UpdateSQLBuilder = {
      if (additionalUpdateSQLs.isEmpty) {
        queryBuilder
      } else {
        val updates = sqls.csv(additionalUpdateSQLs.toSeq*)
        if (othersAreEmpty) queryBuilder.append(updates)
        else queryBuilder.append(sqls", ${updates}")
      }
    }

    /**
     * Updates entities with these permitted strong parameters.
     *
     * @param strongParameters permitted strong parameters
     * @param s                db session
     * @return updated count
     */
    def withPermittedAttributes(
      strongParameters: PermittedStrongParameters
    )(implicit s: DBSession = autoSession): Int = {
      withNamedValues(toNamedValuesToBeUpdated(strongParameters)*)
    }

    /**
     * Updates entities with these non-permitted parameters.
     *
     * CAUTION: If you use this method in some web apps, you might have mass assignment vulnerability.
     *
     * @param parameters unsafe parameters
     * @param s          db session
     * @return updated count
     */
    def withAttributes(
      parameters: (String, Any)*
    )(implicit s: DBSession = autoSession): Int = {
      withNamedValues(parameters.map { case (name, value) =>
        (column.field(name), value)
      }*)
    }

    /**
     * Updates entities with named values.
     *
     * @param namedValues named values
     * @param s           db session
     * @return updated count
     */
    def withNamedValues(
      namedValues: (SQLSyntax, Any)*
    )(implicit s: DBSession = autoSession): Int = {
      val allValues = mergeNamedValues(namedValues)
      beforeHandlers.foreach(_.apply(s, where, allValues))
      val updatedCount = withSQL {
        mergeAdditionalUpdateSQLs(
          update(mapper)
            .set(allValues.map { case (k, v) =>
              k -> AsIsParameterBinder(v)
            }*),
          allValues.isEmpty
        ).where.append(where).and(defaultScopeForUpdateOperations)
      }.update.apply()
      afterHandlers.foreach(_.apply(s, where, allValues, updatedCount))
      updatedCount
    }

  }

  // -------------
  // Delete
  // -------------

  /**
   * Deletes entities by condition.
   *
   * @param where condition
   * @param s     db session
   * @return deleted count
   */
  def deleteBy(where: SQLSyntax)(implicit s: DBSession = autoSession): Int = {
    beforeDeleteByHandlers.foreach(_.apply(s, where))
    val count = withSQL {
      delete.from(this).where(where).and(defaultScopeForUpdateOperations)
    }.update.apply()
    afterDeleteByHandlers.foreach(_.apply(s, where, count))
    count
  }

  /**
   * Deletes all entities.
   */
  def deleteAll()(implicit s: DBSession = autoSession): Int = {
    withSQL {
      delete.from(this)
    }.update.apply()
  }

  /**
   * #updateBy pre-execution handler.
   */
  type BeforeDeleteByHandler = (DBSession, SQLSyntax) => Unit

  /**
   * #updateBy post-execution handler.
   */
  type AfterDeleteByHandler = (DBSession, SQLSyntax, Int) => Unit

  /**
   * Registered beforeUpdateByHandlers.
   */
  protected val beforeDeleteByHandlers =
    new scala.collection.mutable.ListBuffer[BeforeDeleteByHandler]

  /**
   * Registered afterUpdateByHandlers.
   */
  protected val afterDeleteByHandlers =
    new scala.collection.mutable.ListBuffer[AfterDeleteByHandler]

  /**
   * #deleteBy pre-execution.
   *
   * @param handler handler
   */
  protected def beforeDeleteBy(handler: (DBSession, SQLSyntax) => Unit): Unit =
    beforeDeleteByHandlers.append(handler)

  /**
   * #deleteBy post-execution.
   *
   * @param handler handler
   */
  protected def afterDeleteBy(
    handler: (DBSession, SQLSyntax, Int) => Unit
  ): Unit =
    afterDeleteByHandlers.append(handler)

}
