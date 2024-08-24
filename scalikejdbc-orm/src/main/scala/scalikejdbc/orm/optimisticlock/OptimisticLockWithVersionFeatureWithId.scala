package scalikejdbc.orm.optimisticlock

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.crud.CRUDFeatureWithId
import scalikejdbc.orm.exception.OptimisticLockException

/**
  * Optimistic lock with version.
  *
  * @tparam Entity entity
  */
trait OptimisticLockWithVersionFeatureWithId[Id, Entity]
  extends CRUDFeatureWithId[Id, Entity] {

  /**
    * Lock version field name.
    */
  def lockVersionFieldName: String = "lockVersion"

  // add default lockVersion value to creation query
  addAttributeForCreation((column.field(lockVersionFieldName), 1L))

  /**
    * Returns where condition part which search by primary key and lock vesion.
    *
    * @param id primary key
    * @param version lock version
    * @return query part
    */
  def updateByIdAndVersion(id: Long, version: Long): UpdateOperationBuilder = {
    updateBy(
      sqls
        .eq(column.field(primaryKeyFieldName), id)
        .and
        .eq(column.field(lockVersionFieldName), version)
    )
  }

  private[this] def updateByHandler(
    session: DBSession,
    where: SQLSyntax,
    namedValues: Seq[(SQLSyntax, Any)],
    count: Int
  ): Unit = {
    if (count == 0) {
      throw new OptimisticLockException(
        s"Conflict ${lockVersionFieldName} is detected (condition: '${where.value}', ${where.parameters
            .mkString(",")}})"
      )
    }
  }
  afterUpdateBy(updateByHandler)

  override def updateBy(where: SQLSyntax): UpdateOperationBuilder =
    new UpdateOperationBuilderWithVersion(this, where)

  /**
    * Update query builder/executor.
    *
    * @param mapper mapper
    * @param where condition
    */
  class UpdateOperationBuilderWithVersion(
    mapper: CRUDFeatureWithId[Id, Entity],
    where: SQLSyntax
  ) extends UpdateOperationBuilder(
      mapper = mapper,
      where = where,
      beforeHandlers = beforeUpdateByHandlers.toIndexedSeq,
      afterHandlers = afterUpdateByHandlers.toIndexedSeq
    ) {

    // appends additional part of update query
    private[this] val c =
      defaultAlias.support.column.field(lockVersionFieldName)
    addUpdateSQLPart(sqls"${c} = ${c} + 1")
  }

  /**
    * Deletes a single entity by primary key and lock version.
    *
    * @param id primary key
    * @param version lock version
    * @param s db session
    * @return deleted count
    */
  def deleteByIdAndVersion(id: Long, version: Long)(implicit
    s: DBSession = autoSession
  ): Int = {
    deleteBy(
      sqls
        .eq(column.field(primaryKeyFieldName), id)
        .and
        .eq(column.field(lockVersionFieldName), version)
    )
  }

  override def deleteBy(
    where: SQLSyntax
  )(implicit s: DBSession = autoSession): Int = {
    val count = super.deleteBy(where)
    if (count == 0) {
      throw new OptimisticLockException(
        s"Conflict ${lockVersionFieldName} is detected (condition: '${where.value}', ${where.parameters
            .mkString(",")}})"
      )
    } else {
      count
    }
  }

  override def updateById(id: Id): UpdateOperationBuilder = {
    logger.info(
      "#updateById ignore optimistic lock. If you need to lock with version in this case, use #updateBy instead."
    )
    super.updateBy(byId(id))
  }

  override def deleteById(id: Id)(implicit s: DBSession = autoSession): Int = {
    logger.info(
      "#deleteById ignore optimistic lock. If you need to lock with version in this case, use #deleteBy instead."
    )
    super.deleteBy(byId(id))
  }

}
