package scalikejdbc.orm.calculation

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.basic.SQLSyntaxSupportBase

/**
 * Calculation feature.
 */
trait CalculationFeature[Entity] extends SQLSyntaxSupportBase[Entity] {

  /**
   * Calculates rows.
   */
  def calculate(
    sql: SQLSyntax
  )(implicit s: DBSession = autoSession): BigDecimal = {
    withSQL {
      select(sql).from(as(defaultAlias)).where(defaultScopeWithDefaultAlias)
    }.map(_.bigDecimal(1))
      .single
      .apply()
      .map(_.toScalaBigDecimal)
      .getOrElse(BigDecimal(0))
  }

  /**
   * Count only.
   */
  def count(fieldName: String = "", distinct: Boolean = false)(implicit
    s: DBSession = autoSession
  ): Long = {
    if (fieldName == "") {
      withSQL {
        select(sqls.count).from(as(syntax))
      }.map(_.long(1)).single.apply().getOrElse(0L)
    } else {
      calculate {
        if (distinct)
          sqls.count(sqls.distinct(defaultAlias.field(fieldName)))
        else sqls.count(defaultAlias.field(fieldName))
      }.toLong
    }
  }

  /**
   * Counts distinct rows.
   */
  def distinctCount(fieldName: String = primaryKeyFieldName)(implicit
    s: DBSession = autoSession
  ): Long =
    count(fieldName, true)

  /**
   * Calculates sum of a column.
   */
  def sum(fieldName: String)(implicit s: DBSession = autoSession): BigDecimal =
    calculate(sqls.sum(defaultAlias.field(fieldName)))

  /**
   * Calculates average of a column.
   */
  def average(fieldName: String, decimals: Option[Int] = None)(implicit
    s: DBSession = autoSession
  ): BigDecimal = {
    calculate(decimals match {
      case Some(dcml) =>
        val decimalsValue = dcml match {
          case 1 => sqls"1"
          case 2 => sqls"2"
          case 3 => sqls"3"
          case 4 => sqls"4"
          case 5 => sqls"5"
          case 6 => sqls"6"
          case 7 => sqls"7"
          case 8 => sqls"8"
          case 9 => sqls"9"
          case _ => sqls"10"
        }
        sqls"round(${sqls.avg(defaultAlias.field(fieldName))}, ${decimalsValue})"
      case _ =>
        sqls.avg(defaultAlias.field(fieldName))
    })
  }

  def avg(fieldName: String, decimals: Option[Int] = None)(implicit
    s: DBSession = autoSession
  ): BigDecimal =
    average(fieldName, decimals)

  /**
   * Calculates minimum value of a column.
   */
  def minimum(fieldName: String)(implicit
    s: DBSession = autoSession
  ): BigDecimal =
    calculate(sqls.min(defaultAlias.field(fieldName)))

  def min(fieldName: String)(implicit s: DBSession = autoSession): BigDecimal =
    minimum(fieldName)

  /**
   * Calculates minimum value of a column.
   */
  def maximum(fieldName: String)(implicit
    s: DBSession = autoSession
  ): BigDecimal =
    calculate(sqls.max(defaultAlias.field(fieldName)))

  def max(fieldName: String)(implicit s: DBSession = autoSession): BigDecimal =
    maximum(fieldName)

}
