package scalikejdbc.orm.timstamps

import org.joda.time.DateTime
import scalikejdbc.SQLSyntax
import scalikejdbc.orm.basic.SQLSyntaxSupportBase

trait TimestampsFeatureBase[Entity] {
  self: SQLSyntaxSupportBase[Entity] =>

  /**
   * createdAt field name.
   */
  def createdAtFieldName = "createdAt"

  /**
   * updatedAt field name.
   */
  def updatedAtFieldName = "updatedAt"

  protected def timestampValues(
    exists: String => Boolean
  ): Seq[(SQLSyntax, Any)] = {
    val (column, now) = (defaultAlias.support.column, DateTime.now)
    val builder = List.newBuilder[(SQLSyntax, Any)]
    if (!exists(createdAtFieldName))
      builder += column.field(createdAtFieldName) -> now
    if (!exists(updatedAtFieldName))
      builder += column.field(updatedAtFieldName) -> now
    builder.result()
  }

}
