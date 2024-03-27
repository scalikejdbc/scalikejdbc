package scalikejdbc.orm.softdeletion

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm._
import scalikejdbc.orm.crud.CRUDFeatureWithId

/**
  * Soft delete with boolean value.
  *
  * @tparam Entity entity
  */
trait SoftDeleteWithBooleanFeatureWithId[Id, Entity]
  extends CRUDFeatureWithId[Id, Entity] {

  /**
    * is deleted flag field name.
    */
  def isDeletedFieldName: String = "isDeleted"

  override def defaultScopeForUpdateOperations: Option[SQLSyntax] = {
    val c = defaultAlias.support.column
    val scope = sqls.eq(c.field(isDeletedFieldName), false)
    super.defaultScopeForUpdateOperations.map(_.and.append(scope)) orElse Some(
      scope
    )
  }

  override def defaultScope(alias: Alias[Entity]): Option[SQLSyntax] = {
    val scope = sqls.eq(alias.field(isDeletedFieldName), false)
    super.defaultScope(alias).map(_.and.append(scope)) orElse Some(scope)
  }

  override def deleteBy(
    where: SQLSyntax
  )(implicit s: DBSession = autoSession): Int = {
    updateBy(where).withNamedValues(column.field(isDeletedFieldName) -> true)
  }
}
