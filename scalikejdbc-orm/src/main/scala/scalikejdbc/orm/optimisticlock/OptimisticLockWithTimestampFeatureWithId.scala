package scalikejdbc.orm.optimisticlock

import org.joda.time.DateTime

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.crud.CRUDFeatureWithId
import scalikejdbc.orm.exception.OptimisticLockException

/**
  * Optimistic lock with timestamp.
  *
  * @tparam Entity entity
  */
trait OptimisticLockWithTimestampFeatureWithId[Id, Entity]
  extends CRUDFeatureWithId[Id, Entity] {

  /**
    * Lock timestamp field name.
    */
  def lockTimestampFieldName = "lockTimestamp"

  /**
    * Returns where condition part which search by primary key and lock timestamp.
    *
    * @param id primary key
    * @param timestamp lock timestamp
    * @return query part
    */
  protected def byIdAndTimestamp(
    id: Long,
    timestamp: Option[DateTime]
  ): SQLSyntax =
    timestamp
      .map { t =>
        sqls
          .eq(column.field(primaryKeyFieldName), id)
          .and
          .eq(column.field(lockTimestampFieldName), t)
      }
      .getOrElse {
        sqls
          .eq(column.field(primaryKeyFieldName), id)
          .and
          .isNull(column.field(lockTimestampFieldName))
      }

  /**
    * Returns update query builder which updates a single entity by primary key and lock timestamp.
    *
    * @param id primary key
    * @param timestamp lock timestamp
    * @return updated count
    */
  def updateByIdAndTimestamp(
    id: Long,
    timestamp: Option[DateTime]
  ): UpdateOperationBuilder = {
    updateBy(byIdAndTimestamp(id, timestamp))
  }

  /**
    * Returns update query builder which updates a single entity by primary key and lock timestamp.
    *
    * @param id primary key
    * @param timestamp lock timestamp
    * @return updated count
    */
  def updateByIdAndTimestamp(
    id: Long,
    timestamp: DateTime
  ): UpdateOperationBuilder = {
    updateBy(byIdAndTimestamp(id, Option(timestamp)))
  }

  private[this] def updateByHandler(
    session: DBSession,
    where: SQLSyntax,
    namedValues: Seq[(SQLSyntax, Any)],
    count: Int
  ): Unit = {
    if (count == 0) {
      throw new OptimisticLockException(
        s"Conflict ${lockTimestampFieldName} is detected (condition: '${where.value}', ${where.parameters
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
      defaultAlias.support.column.field(lockTimestampFieldName)
    addUpdateSQLPart(sqls"${c} = ${sqls.currentTimestamp}")
  }

  /**
    * Deletes a single entity by primary key and lock timestamp.
    *
    * @param id primary key
    * @param timestamp lock timestamp
    * @param s db session
    * @return deleted count
    */
  def deleteByIdAndOptionalTimestamp(id: Long, timestamp: Option[DateTime])(
    implicit s: DBSession = autoSession
  ): Int = {
    deleteBy(byIdAndTimestamp(id, timestamp))
  }

  /**
    * Deletes a single entity by primary key and lock timestamp.
    *
    * @param id primary key
    * @param timestamp lock timestamp
    * @param s db session
    * @return deleted count
    */
  def deleteByIdAndTimestamp(id: Long, timestamp: DateTime)(implicit
    s: DBSession = autoSession
  ): Int = {
    deleteBy(byIdAndTimestamp(id, Option(timestamp)))
  }

  override def deleteBy(
    where: SQLSyntax
  )(implicit s: DBSession = autoSession): Int = {
    val count = super.deleteBy(where)(s)
    if (count == 0) {
      throw new OptimisticLockException(
        s"Conflict ${lockTimestampFieldName} is detected (condition: '${where.value}', ${where.parameters
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
