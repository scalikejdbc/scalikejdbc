package scalikejdbc
package interpolation

import java.util.Locale.ENGLISH

/**
 * Value as a part of SQL syntax.
 *
 * This value won't be treated as a binding parameter but will be appended as a part of SQL.
 *
 * Note: The constructor should NOT be used by library users at the considerable risk of SQL injection vulnerability.
 * [[https://github.com/scalikejdbc/scalikejdbc/issues/116]]
 */
class SQLSyntax private[scalikejdbc] (
  val value: String,
  private[scalikejdbc] val rawParameters: collection.Seq[Any] = Vector()
) {
  import Implicits._
  import SQLSyntax._

  override def equals(that: Any): Boolean = {
    that match {
      case thatSqls: SQLSyntax =>
        value == thatSqls.value && rawParameters == thatSqls.rawParameters
      case _ =>
        false
    }
  }

  override def hashCode: Int = (value, rawParameters).##

  override def toString(): String =
    s"SQLSyntax(value: ${value}, parameters: ${parameters})"

  lazy val parameters: collection.Seq[Any] = rawParameters.map {
    case ParameterBinder(v) => v
    case x                  => x
  }

  def append(syntax: SQLSyntax): SQLSyntax = sqls"${this} ${syntax}"
  def +(syntax: SQLSyntax): SQLSyntax = this.append(syntax)

  def groupBy(columns: SQLSyntax*): SQLSyntax = {
    if (columns.isEmpty) this else sqls"${this} group by ${toCSV(columns)}"
  }
  def having(condition: SQLSyntax): SQLSyntax =
    sqls"${this} having ${condition}"

  def orderBy(columns: SQLSyntax*): SQLSyntax = {
    if (columns.isEmpty) this else sqls"${this} order by ${toCSV(columns)}"
  }
  def asc: SQLSyntax = sqls"${this} asc"
  def desc: SQLSyntax = sqls"${this} desc"

  def limit(n: Int): SQLSyntax = sqls"${this} limit ${SQLSyntax(n.toString)}"
  def offset(n: Int): SQLSyntax = sqls"${this} offset ${SQLSyntax(n.toString)}"

  def where: SQLSyntax = sqls"${this} where"
  def where(where: SQLSyntax): SQLSyntax = sqls"${this} where ${where}"
  def where(whereOpt: Option[SQLSyntax]): SQLSyntax =
    whereOpt.fold(this)(where(_))

  def and: SQLSyntax = sqls"${this} and"
  def and(sqlPart: SQLSyntax): SQLSyntax = sqls"$this and ($sqlPart)"
  def and(andOpt: Option[SQLSyntax]): SQLSyntax = andOpt.fold(this)(and(_))
  def or: SQLSyntax = sqls"${this} or"
  def or(sqlPart: SQLSyntax): SQLSyntax = sqls"$this or ($sqlPart)"
  def or(orOpt: Option[SQLSyntax]): SQLSyntax = orOpt.fold(this)(or(_))

  def roundBracket(inner: SQLSyntax): SQLSyntax = sqls"$this ($inner)"

  def eq[A](column: SQLSyntax, value: A)(implicit
    ev: ParameterBinderFactory[A]
  ): SQLSyntax = {
    value match {
      case null | None => sqls"${this} ${column} is null"
      case _           => sqls"${this} ${column} = ${ev(value)}"
    }
  }
  def ne[A](column: SQLSyntax, value: A)(implicit
    ev: ParameterBinderFactory[A]
  ): SQLSyntax = {
    value match {
      case null | None => sqls"${this} ${column} is not null"
      case _           => sqls"${this} ${column} <> ${ev(value)}"
    }
  }
  def gt[A](column: SQLSyntax, value: A)(implicit
    ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls"${this} ${column} > ${ev(value)}"
  def ge[A](column: SQLSyntax, value: A)(implicit
    ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls"${this} ${column} >= ${ev(value)}"
  def lt[A](column: SQLSyntax, value: A)(implicit
    ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls"${this} ${column} < ${ev(value)}"
  def le[A](column: SQLSyntax, value: A)(implicit
    ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls"${this} ${column} <= ${ev(value)}"

  def isNull(column: SQLSyntax): SQLSyntax = sqls"${this} ${column} is null"
  def isNotNull(column: SQLSyntax): SQLSyntax =
    sqls"${this} ${column} is not null"

  def between[A, B, C](a: A, b: B, c: C)(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B],
    ev3: ParameterBinderFactory[C]
  ): SQLSyntax = sqls"${this} ${ev1(a)} between ${ev2(b)} and ${ev3(c)}"
  def notBetween[A, B, C](a: A, b: B, c: C)(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B],
    ev3: ParameterBinderFactory[C]
  ): SQLSyntax = sqls"${this} not ${ev1(a)} between ${ev2(b)} and ${ev3(c)}"

  def in[A](column: SQLSyntax, values: collection.Seq[A])(implicit
    ev: ParameterBinderFactory[A]
  ): SQLSyntax = {
    if (values.isEmpty) {
      sqls"${this} FALSE"
    } else {
      sqls"${this} ${column} in (${values.map(ev.apply)})"
    }
  }
  def notIn[A](column: SQLSyntax, values: collection.Seq[A])(implicit
    ev: ParameterBinderFactory[A]
  ): SQLSyntax = {
    if (values.isEmpty) {
      sqls"${this} TRUE"
    } else {
      sqls"${this} ${column} not in (${values.map(ev.apply)})"
    }
  }

  def in(column: SQLSyntax, subQuery: SQLSyntax): SQLSyntax =
    sqls"${this} ${column} in (${subQuery})"
  def notIn(column: SQLSyntax, subQuery: SQLSyntax): SQLSyntax =
    sqls"${this} ${column} not in (${subQuery})"

  def in[A, B](
    columns: (SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B)]
  )(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B]
  ): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} FALSE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value})")
      val values = toCSV(valueSeqs.map { case (v1, v2) =>
        sqls"(${ev1(v1)}, ${ev2(v2)})"
      })
      val inClause = sqls"${column} in (${values})"
      sqls"${this} ${inClause}"
    }
  }
  def notIn[A, B](
    columns: (SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B)]
  )(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B]
  ): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} TRUE"
    } else {
      val column = SQLSyntax(s"(${columns._1.value}, ${columns._2.value})")
      val values = toCSV(valueSeqs.map { case (v1, v2) =>
        sqls"(${ev1(v1)}, ${ev2(v2)})"
      })
      val inClause = sqls"${column} not in (${values})"
      sqls"${this} ${inClause}"
    }
  }

  def in[A, B, C](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C)]
  )(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B],
    ev3: ParameterBinderFactory[C]
  ): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} FALSE"
    } else {
      val column = SQLSyntax(
        s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value})"
      )
      val values = toCSV(valueSeqs.map { case (v1, v2, v3) =>
        sqls"(${ev1(v1)}, ${ev2(v2)}, ${ev3(v3)})"
      })
      val inClause = sqls"${column} in (${values})"
      sqls"${this} ${inClause}"
    }
  }
  def notIn[A, B, C](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C)]
  )(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B],
    ev3: ParameterBinderFactory[C]
  ): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} TRUE"
    } else {
      val column = SQLSyntax(
        s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value})"
      )
      val values = toCSV(valueSeqs.map { case (v1, v2, v3) =>
        sqls"(${ev1(v1)}, ${ev2(v2)}, ${ev3(v3)})"
      })
      val inClause = sqls"${column} not in (${values})"
      sqls"${this} ${inClause}"
    }
  }

  def in[A, B, C, D](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C, D)]
  )(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B],
    ev3: ParameterBinderFactory[C],
    ev4: ParameterBinderFactory[D]
  ): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} FALSE"
    } else {
      val column = SQLSyntax(
        s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value}, ${columns._4.value})"
      )
      val values = toCSV(valueSeqs.map { case (v1, v2, v3, v4) =>
        sqls"(${ev1(v1)}, ${ev2(v2)}, ${ev3(v3)}, ${ev4(v4)})"
      })
      val inClause = sqls"${column} in (${values})"
      sqls"${this} ${inClause}"
    }
  }
  def notIn[A, B, C, D](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C, D)]
  )(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B],
    ev3: ParameterBinderFactory[C],
    ev4: ParameterBinderFactory[D]
  ): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} TRUE"
    } else {
      val column = SQLSyntax(
        s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value}, ${columns._4.value})"
      )
      val values = toCSV(valueSeqs.map { case (v1, v2, v3, v4) =>
        sqls"(${ev1(v1)}, ${ev2(v2)}, ${ev3(v3)}, ${ev4(v4)})"
      })
      val inClause = sqls"${column} not in (${values})"
      sqls"${this} ${inClause}"
    }
  }

  def in[A, B, C, D, E](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C, D, E)]
  )(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B],
    ev3: ParameterBinderFactory[C],
    ev4: ParameterBinderFactory[D],
    ev5: ParameterBinderFactory[E]
  ): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} FALSE"
    } else {
      val column = SQLSyntax(
        s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value}, ${columns._4.value}, ${columns._5.value})"
      )
      val values = toCSV(valueSeqs.map { case (v1, v2, v3, v4, v5) =>
        sqls"(${ev1(v1)}, ${ev2(v2)}, ${ev3(v3)}, ${ev4(v4)}, ${ev5(v5)})"
      })
      val inClause = sqls"${column} in (${values})"
      sqls"${this} ${inClause}"
    }
  }
  def notIn[A, B, C, D, E](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C, D, E)]
  )(implicit
    ev1: ParameterBinderFactory[A],
    ev2: ParameterBinderFactory[B],
    ev3: ParameterBinderFactory[C],
    ev4: ParameterBinderFactory[D],
    ev5: ParameterBinderFactory[E]
  ): SQLSyntax = {
    if (valueSeqs.isEmpty) {
      sqls"${this} TRUE"
    } else {
      val column = SQLSyntax(
        s"(${columns._1.value}, ${columns._2.value}, ${columns._3.value}, ${columns._4.value}, ${columns._5.value})"
      )
      val values = toCSV(valueSeqs.map { case (v1, v2, v3, v4, v5) =>
        sqls"(${ev1(v1)}, ${ev2(v2)}, ${ev3(v3)}, ${ev4(v4)}, ${ev5(v5)})"
      })
      val inClause = sqls"${column} not in (${values})"
      sqls"${this} ${inClause}"
    }
  }

  def like(column: SQLSyntax, value: String): SQLSyntax =
    sqls"${this} ${column} like ${value}"
  def notLike(column: SQLSyntax, value: String): SQLSyntax =
    sqls"${this} ${column} not like ${value}"

  def exists(sqlPart: SQLSyntax): SQLSyntax = sqls"${this} exists (${sqlPart})"
  def notExists(sqlPart: SQLSyntax): SQLSyntax =
    sqls"${this} not exists (${sqlPart})"

  def lower(column: SQLSyntax): SQLSyntax = sqls"lower($column)"
  def upper(column: SQLSyntax): SQLSyntax = sqls"upper($column)"

  def stripMargin: SQLSyntax = new SQLSyntax(value.stripMargin, rawParameters)

  def stripMargin(marginChar: Char): SQLSyntax =
    new SQLSyntax(value.stripMargin(marginChar), rawParameters)

  def ->[A](value: A)(implicit
    ev: ParameterBinderFactory[A]
  ): (SQLSyntax, ParameterBinder) = (this, ev(value))
  def ->(value: ParameterBinder): (SQLSyntax, ParameterBinder) = (this, value)
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
  private[scalikejdbc] def apply(
    value: String,
    parameters: collection.Seq[Any] = Nil
  ): SQLSyntax = new SQLSyntax(value, parameters)

  /**
   * WARNING: Be aware of SQL injection vulnerability.
   */
  def createUnsafely(
    value: String,
    parameters: collection.Seq[Any] = Nil
  ): SQLSyntax = apply(value, parameters)

  def unapply(syntax: SQLSyntax): Some[(String, collection.Seq[Any])] = Some(
    (syntax.value, syntax.rawParameters)
  )

  def join(
    parts: collection.Seq[SQLSyntax],
    delimiter: SQLSyntax,
    spaceBeforeDelimiter: Boolean = true
  ): SQLSyntax = {
    val sep = if (spaceBeforeDelimiter) {
      s" ${delimiter.value} "
    } else {
      s"${delimiter.value} "
    }
    val value =
      parts.collect { case p if p.value.nonEmpty => p.value }.mkString(sep)
    val parameters = if (delimiter.rawParameters.isEmpty) {
      parts.flatMap(_.rawParameters)
    } else {
      parts.tail.foldLeft(
        parts.headOption.fold(collection.Seq.empty[Any])(_.rawParameters)
      ) { case (params, part) =>
        params ++ delimiter.rawParameters ++ part.rawParameters
      }
    }
    apply(value, parameters)
  }
  def csv(parts: SQLSyntax*): SQLSyntax = toCSV(parts)
  private[scalikejdbc] def toCSV(
    parts: scala.collection.Seq[SQLSyntax]
  ): SQLSyntax = join(parts, sqls",", false)

  private[this] def hasAndOr(s: SQLSyntax): Boolean = {
    val statement = s.value.toLowerCase(ENGLISH)
    statement.matches(".+\\s+and\\s+.+") ||
    statement.matches(".+\\s+or\\s+.+")
  }

  def joinWithAnd(parts: SQLSyntax*): SQLSyntax =
    join(parts.map(p => if (hasAndOr(p)) sqls"(${p})" else p), sqls"and")
  def joinWithOr(parts: SQLSyntax*): SQLSyntax =
    join(parts.map(p => if (hasAndOr(p)) sqls"(${p})" else p), sqls"or")

  def groupBy(columns: SQLSyntax*): SQLSyntax =
    SQLSyntax.empty.groupBy(columns.filterNot(_.value.trim.isEmpty)*)
  def having(condition: SQLSyntax): SQLSyntax =
    SQLSyntax.empty.having(condition)

  def orderBy(columns: SQLSyntax*): SQLSyntax =
    SQLSyntax.empty.orderBy(columns.filterNot(_.value.trim.isEmpty)*)
  val asc: SQLSyntax = SQLSyntax.empty.asc
  val desc: SQLSyntax = SQLSyntax.empty.desc

  def limit(n: Int): SQLSyntax = SQLSyntax.empty.limit(n)
  def offset(n: Int): SQLSyntax = SQLSyntax.empty.offset(n)

  val where: SQLSyntax = SQLSyntax.empty.where
  def where(where: SQLSyntax): SQLSyntax = SQLSyntax.empty.where(where)
  def where(whereOpt: Option[SQLSyntax]): SQLSyntax =
    SQLSyntax.empty.where(whereOpt)

  def and: SQLSyntax = SQLSyntax.empty.and
  def and(sqlPart: SQLSyntax): SQLSyntax = SQLSyntax.empty.and(sqlPart)
  def and(andOpt: Option[SQLSyntax]): SQLSyntax = SQLSyntax.empty.and(andOpt)
  def or: SQLSyntax = SQLSyntax.empty.or
  def or(sqlPart: SQLSyntax): SQLSyntax = SQLSyntax.empty.or(sqlPart)
  def or(orOpt: Option[SQLSyntax]): SQLSyntax = SQLSyntax.empty.or(orOpt)

  def eq[A: ParameterBinderFactory](column: SQLSyntax, value: A): SQLSyntax =
    SQLSyntax.empty.eq(column, value)
  def ne[A: ParameterBinderFactory](column: SQLSyntax, value: A): SQLSyntax =
    SQLSyntax.empty.ne(column, value)
  def gt[A: ParameterBinderFactory](column: SQLSyntax, value: A): SQLSyntax =
    SQLSyntax.empty.gt(column, value)
  def ge[A: ParameterBinderFactory](column: SQLSyntax, value: A): SQLSyntax =
    SQLSyntax.empty.ge(column, value)
  def lt[A: ParameterBinderFactory](column: SQLSyntax, value: A): SQLSyntax =
    SQLSyntax.empty.lt(column, value)
  def le[A: ParameterBinderFactory](column: SQLSyntax, value: A): SQLSyntax =
    SQLSyntax.empty.le(column, value)

  def isNull(column: SQLSyntax): SQLSyntax = SQLSyntax.empty.isNull(column)
  def isNotNull(column: SQLSyntax): SQLSyntax =
    SQLSyntax.empty.isNotNull(column)
  def between[A: ParameterBinderFactory, B: ParameterBinderFactory](
    column: SQLSyntax,
    a: A,
    b: B
  ): SQLSyntax = SQLSyntax.empty.between(column, a, b)
  def notBetween[A: ParameterBinderFactory, B: ParameterBinderFactory](
    column: SQLSyntax,
    a: A,
    b: B
  ): SQLSyntax = SQLSyntax.empty.notBetween(column, a, b)

  def in[A: ParameterBinderFactory](
    column: SQLSyntax,
    values: collection.Seq[A]
  ): SQLSyntax = SQLSyntax.empty.in(column, values)
  def notIn[A: ParameterBinderFactory](
    column: SQLSyntax,
    values: collection.Seq[A]
  ): SQLSyntax = SQLSyntax.empty.notIn(column, values)

  def in[A: ParameterBinderFactory, B: ParameterBinderFactory](
    columns: (SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B)]
  ): SQLSyntax = SQLSyntax.empty.in(columns, valueSeqs)
  def notIn[A: ParameterBinderFactory, B: ParameterBinderFactory](
    columns: (SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B)]
  ): SQLSyntax = SQLSyntax.empty.notIn(columns, valueSeqs)

  def in[
    A: ParameterBinderFactory,
    B: ParameterBinderFactory,
    C: ParameterBinderFactory
  ](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C)]
  ): SQLSyntax = SQLSyntax.empty.in(columns, valueSeqs)
  def notIn[
    A: ParameterBinderFactory,
    B: ParameterBinderFactory,
    C: ParameterBinderFactory
  ](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C)]
  ): SQLSyntax = SQLSyntax.empty.notIn(columns, valueSeqs)

  def in[
    A: ParameterBinderFactory,
    B: ParameterBinderFactory,
    C: ParameterBinderFactory,
    D: ParameterBinderFactory
  ](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C, D)]
  ): SQLSyntax = SQLSyntax.empty.in(columns, valueSeqs)
  def notIn[
    A: ParameterBinderFactory,
    B: ParameterBinderFactory,
    C: ParameterBinderFactory,
    D: ParameterBinderFactory
  ](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C, D)]
  ): SQLSyntax = SQLSyntax.empty.notIn(columns, valueSeqs)

  def in[
    A: ParameterBinderFactory,
    B: ParameterBinderFactory,
    C: ParameterBinderFactory,
    D: ParameterBinderFactory,
    E: ParameterBinderFactory
  ](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C, D, E)]
  ): SQLSyntax = SQLSyntax.empty.in(columns, valueSeqs)
  def notIn[
    A: ParameterBinderFactory,
    B: ParameterBinderFactory,
    C: ParameterBinderFactory,
    D: ParameterBinderFactory,
    E: ParameterBinderFactory
  ](
    columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
    valueSeqs: collection.Seq[(A, B, C, D, E)]
  ): SQLSyntax = SQLSyntax.empty.notIn(columns, valueSeqs)

  def in(column: SQLSyntax, subQuery: SQLSyntax): SQLSyntax =
    SQLSyntax.empty.in(column, subQuery)
  def notIn(column: SQLSyntax, subQuery: SQLSyntax): SQLSyntax =
    SQLSyntax.empty.notIn(column, subQuery)

  def like(column: SQLSyntax, value: String): SQLSyntax =
    SQLSyntax.empty.like(column, value)
  def notLike(column: SQLSyntax, value: String): SQLSyntax =
    SQLSyntax.empty.notLike(column, value)

  def exists(sqlPart: SQLSyntax): SQLSyntax = SQLSyntax.empty.exists(sqlPart)
  def notExists(sqlPart: SQLSyntax): SQLSyntax =
    SQLSyntax.empty.notExists(sqlPart)

  def lower(column: SQLSyntax): SQLSyntax = SQLSyntax.empty.lower(column)
  def upper(column: SQLSyntax): SQLSyntax = SQLSyntax.empty.upper(column)

  def distinct(columns: SQLSyntax*): SQLSyntax =
    sqls"distinct ${toCSV(columns)}"

  def avg(column: SQLSyntax): SQLSyntax = sqls"avg(${column})"

  val count: SQLSyntax = sqls"count(1)"
  def count(column: SQLSyntax): SQLSyntax = sqls"count(${column})"
  def count(asteriskProvider: AsteriskProvider): SQLSyntax =
    sqls"count(${asteriskProvider.asterisk})"

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
    if (cs.isEmpty) None else Some(joinWithAnd(cs*))
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
    if (cs.isEmpty) None else Some(joinWithOr(cs*))
  }

  def roundBracket(inner: SQLSyntax): SQLSyntax = sqls"($inner)"

}
