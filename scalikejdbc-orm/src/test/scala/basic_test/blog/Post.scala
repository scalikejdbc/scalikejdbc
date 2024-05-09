package basic_test.blog

import org.joda.time._
import scalikejdbc._
import scalikejdbc.orm.CRUDMapper
import scalikejdbc.orm.timstamps.TimestampsFeature

case class Post(
  id: Long,
  title: String,
  body: String,
  viewCount: BigDecimal,
  tags: Seq[Tag] = Nil,
  createdAt: DateTime,
  updatedAt: Option[DateTime] = None
)

object Post extends CRUDMapper[Post] with TimestampsFeature[Post] {
  override val connectionPoolName: Any = "blog"
  override val tableName = "posts"
  override val defaultAlias = createAlias("p")

  val tagsRef = hasManyThrough[Tag](
    through = PostTag,
    many = Tag,
    merge = (p, t) => p.copy(tags = t)
  ) // .byDefault

  override def extract(rs: WrappedResultSet, rn: ResultName[Post]): Post =
    autoConstruct(rs, rn, "tags")
}
