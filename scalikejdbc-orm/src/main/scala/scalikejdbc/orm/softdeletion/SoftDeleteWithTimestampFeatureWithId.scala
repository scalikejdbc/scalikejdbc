package scalikejdbc.orm.softdeletion

import org.joda.time.DateTime

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.Alias
import scalikejdbc.orm.crud.CRUDFeatureWithId

/**
  * Soft delete with timestamp value.
  *
  * @tparam Entity entity
  */
trait SoftDeleteWithTimestampFeatureWithId[Id, Entity]
  extends CRUDFeatureWithId[Id, Entity] {

  /**
    * deleted_at timestamp field name.
    */
  def deletedAtFieldName: String = "deletedAt"

  override def defaultScopeForUpdateOperations: Option[SQLSyntax] = {
    val c = defaultAlias.support.column
    val scope = sqls.isNull(c.field(deletedAtFieldName))
    super.defaultScopeForUpdateOperations.map(_.and.append(scope)) orElse Some(
      scope
    )
  }

  override def defaultScope(alias: Alias[Entity]): Option[SQLSyntax] = {
    val scope = sqls.isNull(alias.field(deletedAtFieldName))
    super.defaultScope(alias).map(_.and.append(scope)) orElse Some(scope)
  }

  override def deleteBy(
    where: SQLSyntax
  )(implicit s: DBSession = autoSession): Int = {
    updateBy(where).withNamedValues(
      column.field(deletedAtFieldName) -> DateTime.now
    )
  }
}
