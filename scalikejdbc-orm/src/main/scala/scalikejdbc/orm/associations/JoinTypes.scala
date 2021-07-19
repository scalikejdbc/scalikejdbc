package scalikejdbc.orm.associations

/**
  * Join type.
  */
sealed trait JoinType

/**
  * Inner join.
  */
case object InnerJoin extends JoinType

/**
  * Left (outer) join.
  */
case object LeftOuterJoin extends JoinType

// right join is currently unsupported.
