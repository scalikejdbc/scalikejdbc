package basic_test.blog

import org.joda.time._
import scalikejdbc._
import scalikejdbc.orm.CRUDMapper
import scalikejdbc.orm.timstamps.TimestampsFeature

case class Tag(
  id: Long,
  name: String,
  createdAt: DateTime,
  updatedAt: Option[DateTime] = None
)

object Tag extends CRUDMapper[Tag] with TimestampsFeature[Tag] {
  override val connectionPoolName: Any = "blog"
  override val tableName = "tags"
  override val defaultAlias = createAlias("t")

  override def extract(rs: WrappedResultSet, rn: ResultName[Tag]): Tag =
    autoConstruct(rs, rn)
}
