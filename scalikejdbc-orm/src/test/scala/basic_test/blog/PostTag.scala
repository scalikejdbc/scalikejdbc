package basic_test.blog

import org.joda.time._
import scalikejdbc._
import scalikejdbc.orm.JoinTable

case class PostTag(
  id: Long,
  tagId: Int,
  postId: Int,
  createdAt: DateTime
)

object PostTag extends JoinTable[PostTag] {
  override val connectionPoolName: Any = "blog"
  override val tableName = "posts_tags"
  override val defaultAlias = createAlias("pt")

  override def extract(rs: WrappedResultSet, rn: ResultName[PostTag]): PostTag =
    autoConstruct(rs, rn)
}
