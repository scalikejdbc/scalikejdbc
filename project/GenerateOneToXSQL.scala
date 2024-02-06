object GenerateOneToXSQL {
  val value: String = s"""package scalikejdbc

/**
 * Endpoint of one-to-x APIs
 */
class OneToXSQL[A, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: scala.collection.Seq[Any])(val one: WrappedResultSet => A)
  extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-one/one-to-many operation needs toOne(RS => Option[B]).map((A,B) => A) or toMany(RS => Option[B]).map((A,Seq(B) => A)."))
  with AllOutputDecisionsUnsupported[Z, E] {

  def toOne[B](to: WrappedResultSet => B): OneToOneSQL[A, B, E, Z] = {
    val q: OneToOneSQL[A, B, E, Z] = new OneToOneSQL(statement, rawParameters)(one)(to.andThen((b: B) => Option(b)))((a, b) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq*)
    q
  }

  def toOptionalOne[B](to: WrappedResultSet => Option[B]): OneToOneSQL[A, Option[B], E, Z] = {
    val q: OneToOneSQL[A, Option[B], E, Z] = new OneToOneSQL(statement, rawParameters)(one)(to.andThen((maybeB: Option[B]) => Option(maybeB)))((a, maybeB) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq*)
    q
  }

  def toMany[B](to: WrappedResultSet => Option[B]): OneToManySQL[A, B, E, Z] = {
    val q: OneToManySQL[A, B, E, Z] = new OneToManySQL(statement, rawParameters)(one)(to)((a, bs) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq*)
    q
  }

${(2 to 21).map(manies).mkString("\n")}

}

object OneToXSQL {
  def unapply[A, E <: WithExtractor, Z](sqlObject: OneToXSQL[A, E, Z]): Some[(String, scala.collection.Seq[Any], WrappedResultSet => A)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one))
  }
  def handleException(e: Exception): Nothing = e match {
    case invalidColumn: InvalidColumnNameException =>
      throw new ResultSetExtractorException(
        "Failed to extract ResultSet because the specified column name (" + invalidColumn.name + ") is invalid." +
          " If you're using SQLInterpolation, you may mistake u.id for u.resultName.id.")
    case e: Exception => throw e
  }
}
"""

  private[this] def tparam(n: Int) = (1 to n).map("B" + _).mkString(", ")

  private[this] def manies(n: Int): String = s"""  def toManies[${tparam(n)}](
    ${(1 to n)
      .map(x => s"to${x}: WrappedResultSet => Option[B${x}]")
      .mkString(", ")}
  ): OneToManies${n}SQL[A, ${tparam(n)}, E, Z] = {
    val q: OneToManies${n}SQL[A, ${tparam(n)}, E, Z] = new OneToManies${n}SQL(
      statement, rawParameters)(one)(${(1 to n)
      .map("to" + _)
      .mkString(", ")})((a, ${List
      .fill(n)("_")
      .mkString(", ")}) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq*)
  }
"""
}
