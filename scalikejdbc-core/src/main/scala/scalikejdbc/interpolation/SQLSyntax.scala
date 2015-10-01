package scalikejdbc.interpolation

/**
 * Value as a part of SQL syntax.
 *
 * This value won't be treated as a binding parameter but will be appended as a part of SQL.
 *
 * Note: The constructor should NOT be used by library users at the considerable risk of SQL injection vulnerability.
 * [[https://github.com/scalikejdbc/scalikejdbc/issues/116]]
 */
class SQLSyntax private[scalikejdbc] (val value: String, val parameters: Seq[Any] = Vector()) {
  import Implicits._
  import SQLSyntax._

  override def equals(that: Any): Boolean = {
    if (that.isInstanceOf[SQLSyntax]) {
      val thatSqls = that.asInstanceOf[SQLSyntax]
      value == thatSqls.value && parameters == thatSqls.parameters
    } else {
      false
    }
  }

  override def hashCode: Int = (value, parameters).##

  override def toString(): String = s"SQLSyntax(value: ${value}, parameters: ${parameters})"

  def append(syntax: SQLSyntax): SQLSyntax = sqls"${this} ${syntax}"

  def groupBy(columns: SQLSyntax*): SQLSyntax = {
    if (columns.isEmpty) this else sqls"${this} group by ${csv(columns: _*)}"
  }
  def having(condition: SQLSyntax): SQLSyntax = sqls"${this} having ${condition}"

  def orderBy(columns: SQLSyntax*): SQLSyntax = {
    if (columns.isEmpty) this else sqls"${this} order by ${csv(columns: _*)}"
  }
  def asc = sqls"${this} asc"
  def desc = sqls"${this} desc"

  def limit(n: Int): SQLSyntax = sqls"${this} limit ${SQLSyntax(n.toString)}"
  def offset(n: Int): SQLSyntax = sqls"${this} offset ${SQLSyntax(n.toString)}"

  def where = sqls"${this} where"
  def where(where: SQLSyntax): SQLSyntax = sqls"${this} where ${where}"
  def where(whereOpt: Option[SQLSyntax]): SQLSyntax = whereOpt.fold(this)(where(_))

  def and = sqls"${this} and"
  def and(sqlPart: SQLSyntax): SQLSyntax = sqls"$this and ($sqlPart)"
  def and(andOpt: Option[SQLSyntax]): SQLSyntax = andOpt.fold(this)(and(_))
  def or = sqls"${this} or"
  def or(sqlPart: SQLSyntax): SQLSyntax = sqls"$this or ($sqlPart)"
  def or(orOpt: Option[SQLSyntax]): SQLSyntax = orOpt.fold(this)(or(_))

  def roundBracket(inner: SQLSyntax) = sqls"$this ($inner)"

  def eq(column: SQLSyntax, value: Any): SQLSyntax = {
    value match {
      case null | None => sqls"${this} ${column} is null"
      case _ => sqls"${this} ${column} = ${value}"
    }
  }
  def ne(column: SQLSyntax, value: Any): SQLSyntax = {
    value match {
      case null | None => sqls"${this} ${column} is not null"
      case _ => sqls"${this} ${column} <> ${value}"
    }
  }
  def gt(column: SQLSyntax, value: Any): SQLSyntax = sqls"${this} ${column} > ${value}"
  def ge(column: SQLSyntax, value: Any): SQLSyntax = sqls"${this} ${column} >= ${value}"
  def lt(column: SQLSyntax, value: Any): SQLSyntax = sqls"${this} ${column} < ${value}"
  def le(column: SQLSyntax, value: Any): SQLSyntax = sqls"${this} ${column} <= ${value}"

  def isNull(column: SQLSyntax): SQLSyntax = sqls"${this} ${column} is null"
  def isNotNull(column: SQLSyntax): SQLSyntax = sqls"${this} ${column} is not null"
  def between(column: SQLSyntax, a: Any, b: Any): SQLSyntax = sqls"${this} ${column} between ${a} and ${b}"
  def notBetween(column: SQLSyntax, a: Any, b: Any): SQLSyntax = sqls"${this} ${column} not between ${a} and ${b}"

  def in(column: SQLSyntax, values: Seq[Any]): SQLSyntax = {
    if (values.isEmpty) {
      sqls"${this} FALSE"
    } else {
      sqls"${this} ${column} in (${values})"
    }
  }
  def notIn(column: SQLSyntax, values: Seq[Any]): SQLSyntax = {
    if (values.isEmpty) {
      sqls"${this} TRUE"
    } else {
      sqls"${this} ${column} not in (${values})"
    }
  }

  def in(column: SQLSyntax, subQuery: SQLSyntax): SQLSyntax = sqls"${this} ${column} in (${subQuery})"
  def notIn(column: SQLSyntax, subQuery: SQLSyntax): SQLSyntax = sqls"${this} ${column} not in (${subQuery})"

  def in(columns: (SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any)]): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} FALSE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value})")
      val values = csv(valueSeqs.map { case (v1, v2) => sqls"($v1, $v2)" }: _*)
      val inClause = sqls"${column} in (${values})"
      sqls"${this} ${inClause}"
    }
  }
  def notIn(columns: (SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any)]): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} TRUE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value})")
      val values = csv(valueSeqs.map { case (v1, v2) => sqls"($v1, $v2)" }: _*)
      val inClause = sqls"${column} not in (${values})"
      sqls"${this} ${inClause}"
    }
  }

  def in(columns: (SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any)]): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} FALSE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value})")
      val values = csv(valueSeqs.map { case (v1, v2, v3) => sqls"($v1, $v2, $v3)" }: _*)
      val inClause = sqls"${column} in (${values})"
      sqls"${this} ${inClause}"
    }
  }
  def notIn(columns: (SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any)]): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} TRUE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value})")
      val values = csv(valueSeqs.map { case (v1, v2, v3) => sqls"($v1, $v2, $v3)" }: _*)
      val inClause = sqls"${column} not in (${values})"
      sqls"${this} ${inClause}"
    }
  }

  def in(columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any, Any)]): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} FALSE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value}, ${columns._4.value})")
      val values = csv(valueSeqs.map { case (v1, v2, v3, v4) => sqls"($v1, $v2, $v3, $v4)" }: _*)
      val inClause = sqls"${column} in (${values})"
      sqls"${this} ${inClause}"
    }
  }
  def notIn(columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any, Any)]): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} TRUE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value}, ${columns._4.value})")
      val values = csv(valueSeqs.map { case (v1, v2, v3, v4) => sqls"($v1, $v2, $v3, $v4)" }: _*)
      val inClause = sqls"${column} not in (${values})"
      sqls"${this} ${inClause}"
    }
  }

  def in(columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any, Any, Any)]): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} FALSE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value}, ${columns._4.value}, ${columns._5.value})")
      val values = csv(valueSeqs.map { case (v1, v2, v3, v4, v5) => sqls"($v1, $v2, $v3, $v4, $v5)" }: _*)
      val inClause = sqls"${column} in (${values})"
      sqls"${this} ${inClause}"
    }
  }
  def notIn(columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any, Any, Any)]): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} TRUE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value}, ${columns._4.value}, ${columns._5.value})")
      val values = csv(valueSeqs.map { case (v1, v2, v3, v4, v5) => sqls"($v1, $v2, $v3, $v4, $v5)" }: _*)
      val inClause = sqls"${column} not in (${values})"
      sqls"${this} ${inClause}"
    }
  }

  def like(column: SQLSyntax, value: String): SQLSyntax = sqls"${this} ${column} like ${value}"
  def notLike(column: SQLSyntax, value: String): SQLSyntax = sqls"${this} ${column} not like ${value}"

  def exists(sqlPart: SQLSyntax): SQLSyntax = sqls"${this} exists (${sqlPart})"
  def notExists(sqlPart: SQLSyntax): SQLSyntax = sqls"${this} not exists (${sqlPart})"

  def stripMargin: SQLSyntax = new SQLSyntax(value.stripMargin, parameters)

  def stripMargin(marginChar: Char): SQLSyntax = new SQLSyntax(value.stripMargin(marginChar), parameters)

}

/**
 * A trait which has #resultAll: SQLSyntax
 */
trait ResultAllProvider {
  def resultAll: SQLSyntax
}

/**
 * A trait which has #asterisk: SQLSyntax
 */
trait AsteriskProvider {
  def asterisk: SQLSyntax
}

/*
 * SQLSyntax companion object
 */
object SQLSyntax {

  import Implicits._

  val empty: SQLSyntax = sqls""

  // #apply method should NOT be used by library users at the considerable risk of SQL injection vulnerability.
  // https://github.com/scalikejdbc/scalikejdbc/issues/116
  private[scalikejdbc] def apply(value: String, parameters: Seq[Any] = Nil): SQLSyntax = new SQLSyntax(value, parameters)

  /**
   * WARNING: Be aware of SQL injection vulnerability.
   */
  def createUnsafely(value: String, parameters: Seq[Any] = Nil): SQLSyntax = apply(value, parameters)

  def unapply(syntax: SQLSyntax): Option[(String, Seq[Any])] = Some((syntax.value, syntax.parameters))

  def join(parts: Seq[SQLSyntax], delimiter: SQLSyntax, spaceBeforeDelimier: Boolean = true): SQLSyntax = {
    val sep = if (spaceBeforeDelimier) {
      s" ${delimiter.value} "
    } else {
      s"${delimiter.value} "
    }
    val value = parts.map(_.value).mkString(sep)
    val parameters = if (delimiter.parameters.isEmpty) {
      parts.flatMap(_.parameters)
    } else {
      parts.tail.foldLeft(parts.headOption.fold(Seq.empty[Any])(_.parameters)) {
        case (params, part) => params ++ delimiter.parameters ++ part.parameters
      }
    }
    apply(value, parameters)
  }
  def csv(parts: SQLSyntax*): SQLSyntax = join(parts, sqls",", false)

  private[this] def hasAndOr(s: SQLSyntax): Boolean = {
    val statement = s.value.toLowerCase
    statement.matches(".+\\s+and\\s+.+") ||
      statement.matches(".+\\s+or\\s+.+")
  }

  def joinWithAnd(parts: SQLSyntax*): SQLSyntax = join(parts.map(p => if (hasAndOr(p)) sqls"(${p})" else p), sqls"and")
  def joinWithOr(parts: SQLSyntax*): SQLSyntax = join(parts.map(p => if (hasAndOr(p)) sqls"(${p})" else p), sqls"or")

  def groupBy(columns: SQLSyntax*): SQLSyntax = SQLSyntax.empty.groupBy(columns.filterNot(_.value.trim.isEmpty): _*)
  def having(condition: SQLSyntax): SQLSyntax = SQLSyntax.empty.having(condition)

  def orderBy(columns: SQLSyntax*): SQLSyntax = SQLSyntax.empty.orderBy(columns.filterNot(_.value.trim.isEmpty): _*)
  val asc: SQLSyntax = SQLSyntax.empty.asc
  val desc: SQLSyntax = SQLSyntax.empty.desc

  def limit(n: Int): SQLSyntax = SQLSyntax.empty.limit(n)
  def offset(n: Int): SQLSyntax = SQLSyntax.empty.offset(n)

  val where: SQLSyntax = SQLSyntax.empty.where
  def where(where: SQLSyntax): SQLSyntax = SQLSyntax.empty.where(where)
  def where(whereOpt: Option[SQLSyntax]): SQLSyntax = SQLSyntax.empty.where(whereOpt)

  def and: SQLSyntax = SQLSyntax.empty.and
  def and(sqlPart: SQLSyntax): SQLSyntax = SQLSyntax.empty.and(sqlPart)
  def and(andOpt: Option[SQLSyntax]): SQLSyntax = SQLSyntax.empty.and(andOpt)
  def or: SQLSyntax = SQLSyntax.empty.or
  def or(sqlPart: SQLSyntax): SQLSyntax = SQLSyntax.empty.or(sqlPart)
  def or(orOpt: Option[SQLSyntax]): SQLSyntax = SQLSyntax.empty.or(orOpt)

  def eq(column: SQLSyntax, value: Any): SQLSyntax = SQLSyntax.empty.eq(column, value)
  def ne(column: SQLSyntax, value: Any): SQLSyntax = SQLSyntax.empty.ne(column, value)
  def gt(column: SQLSyntax, value: Any): SQLSyntax = SQLSyntax.empty.gt(column, value)
  def ge(column: SQLSyntax, value: Any): SQLSyntax = SQLSyntax.empty.ge(column, value)
  def lt(column: SQLSyntax, value: Any): SQLSyntax = SQLSyntax.empty.lt(column, value)
  def le(column: SQLSyntax, value: Any): SQLSyntax = SQLSyntax.empty.le(column, value)

  def isNull(column: SQLSyntax): SQLSyntax = SQLSyntax.empty.isNull(column)
  def isNotNull(column: SQLSyntax): SQLSyntax = SQLSyntax.empty.isNotNull(column)
  def between(column: SQLSyntax, a: Any, b: Any): SQLSyntax = SQLSyntax.empty.between(column, a, b)
  def notBetween(column: SQLSyntax, a: Any, b: Any): SQLSyntax = SQLSyntax.empty.notBetween(column, a, b)

  def in(column: SQLSyntax, values: Seq[Any]): SQLSyntax = SQLSyntax.empty.in(column, values)
  def notIn(column: SQLSyntax, values: Seq[Any]): SQLSyntax = SQLSyntax.empty.notIn(column, values)

  def in(columns: (SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any)]): SQLSyntax = SQLSyntax.empty.in(columns, valueSeqs)
  def notIn(columns: (SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any)]): SQLSyntax = SQLSyntax.empty.notIn(columns, valueSeqs)

  def in(columns: (SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any)]): SQLSyntax = SQLSyntax.empty.in(columns, valueSeqs)
  def notIn(columns: (SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any)]): SQLSyntax = SQLSyntax.empty.notIn(columns, valueSeqs)

  def in(columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any, Any)]): SQLSyntax = SQLSyntax.empty.in(columns, valueSeqs)
  def notIn(columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any, Any)]): SQLSyntax = SQLSyntax.empty.notIn(columns, valueSeqs)

  def in(columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any, Any, Any)]): SQLSyntax = SQLSyntax.empty.in(columns, valueSeqs)
  def notIn(columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax), valueSeqs: Seq[(Any, Any, Any, Any, Any)]): SQLSyntax = SQLSyntax.empty.notIn(columns, valueSeqs)

  def in(column: SQLSyntax, subQuery: SQLSyntax): SQLSyntax = SQLSyntax.empty.in(column, subQuery)
  def notIn(column: SQLSyntax, subQuery: SQLSyntax): SQLSyntax = SQLSyntax.empty.notIn(column, subQuery)

  def like(column: SQLSyntax, value: String): SQLSyntax = SQLSyntax.empty.like(column, value)
  def notLike(column: SQLSyntax, value: String): SQLSyntax = SQLSyntax.empty.notLike(column, value)

  def exists(sqlPart: SQLSyntax): SQLSyntax = SQLSyntax.empty.exists(sqlPart)
  def notExists(sqlPart: SQLSyntax): SQLSyntax = SQLSyntax.empty.notExists(sqlPart)

  def distinct(columns: SQLSyntax*): SQLSyntax = sqls"distinct ${csv(columns: _*)}"

  def avg(column: SQLSyntax): SQLSyntax = sqls"avg(${column})"

  val count: SQLSyntax = sqls"count(1)"
  def count(column: SQLSyntax): SQLSyntax = sqls"count(${column})"
  def count(asteriskProvider: AsteriskProvider): SQLSyntax = sqls"count(${asteriskProvider.asterisk})"

  def min(column: SQLSyntax): SQLSyntax = sqls"min(${column})"
  def max(column: SQLSyntax): SQLSyntax = sqls"max(${column})"
  def sum(column: SQLSyntax): SQLSyntax = sqls"sum(${column})"

  def abs(column: SQLSyntax): SQLSyntax = sqls"abs(${column})"
  def floor(column: SQLSyntax): SQLSyntax = sqls"floor(${column})"
  def ceil(column: SQLSyntax): SQLSyntax = sqls"ceil(${column})"
  def ceiling(column: SQLSyntax): SQLSyntax = sqls"ceiling(${column})"

  val ? : SQLSyntax = sqls"?"

  val currentDate: SQLSyntax = sqls"current_date"
  val currentTimestamp: SQLSyntax = sqls"current_timestamp"
  val dual: SQLSyntax = sqls"dual"

  /**
   * Returns an optional SQLSyntax which is flatten (from option array) and joined with 'and'.
   *
   * {{{
   *   val (id, name) = (123, "Alice")
   *   val cond: Option[SQLSyntax] = SQLSyntax.toAndConditionOpt(Some(sqls"id = ${id}"), None, Some(sqls"name = ${name} or name is null"))
   *   cond.get.value // "id = ? and (name = ? or name is null)"
   *   cond.get.parameters // Seq(123, "Alice")
   * }}}
   */
  def toAndConditionOpt(conditions: Option[SQLSyntax]*): Option[SQLSyntax] = {
    val cs: Seq[SQLSyntax] = conditions.flatten
    if (cs.isEmpty) None else Some(joinWithAnd(cs: _*))
  }

  /**
   * Returns an optional SQLSyntax which is flatten (from option array) and joined with 'or'.
   *
   * {{{
   *   val (id, name) = (123, "Alice")
   *   val cond: Option[SQLSyntax] = SQLSyntax.toOrConditionOpt(Some(sqls"id = ${id}"), None, Some(sqls"name = ${name} or name is null"))
   *   cond.get.value // "id = ? or (name = ? or name is null)"
   *   cond.get.parameters // Seq(123, "Alice")
   * }}}
   */
  def toOrConditionOpt(conditions: Option[SQLSyntax]*): Option[SQLSyntax] = {
    val cs: Seq[SQLSyntax] = conditions.flatten
    if (cs.isEmpty) None else Some(joinWithOr(cs: _*))
  }

  def roundBracket(inner: SQLSyntax) = sqls"($inner)"

}

