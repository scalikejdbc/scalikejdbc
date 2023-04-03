package scalikejdbc
package jodatime

import java.sql.ResultSet
import org.joda.time.{
  LocalDateTime => JodaLocalDateTime,
  LocalTime => JodaLocalTime,
  LocalDate => JodaLocalDate,
  _
}

import JodaTypeBinder._
import scala.language.implicitConversions

/**
 * java.sql.ResultSet wrapper.
 */
case class JodaWrappedResultSet(
  underlying: ResultSet,
  cursor: ResultSetCursor,
  index: Int
) {
  private[this] def ensureCursor(): Unit = {
    if (cursor.position != index) {
      throw new IllegalStateException(
        ErrorMessage.INVALID_CURSOR_POSITION + " (actual:" + cursor.position + ",expected:" + index + ")"
      )
    }
  }

  /**
   * Throws ResultSetExtractorException if some exception is thrown.
   */
  private[this] def wrapIfError[A](op: => A): A = {
    try {
      op
    } catch {
      case e: Exception =>
        throw ResultSetExtractorException(
          "Failed to retrieve value because " + e.getMessage + ". If you're using SQLInterpolation, you may mistake u.id for u.resultName.id.",
          Some(e)
        )
    }
  }

  def jodaDateTime(columnIndex: Int): DateTime = get[DateTime](columnIndex)
  def jodaDateTime(columnLabel: String): DateTime = get[DateTime](columnLabel)

  def jodaLocalDate(columnIndex: Int): JodaLocalDate =
    get[JodaLocalDate](columnIndex)
  def jodaLocalDate(columnLabel: String): JodaLocalDate =
    get[JodaLocalDate](columnLabel)

  def jodaLocalTime(columnIndex: Int): JodaLocalTime =
    get[JodaLocalTime](columnIndex)
  def jodaLocalTime(columnLabel: String): JodaLocalTime =
    get[JodaLocalTime](columnLabel)

  def jodaLocalDateTime(columnIndex: Int): JodaLocalDateTime =
    get[JodaLocalDateTime](columnIndex)
  def jodaLocalDateTime(columnLabel: String): JodaLocalDateTime =
    get[JodaLocalDateTime](columnLabel)

  def jodaDateTimeOpt(columnIndex: Int): Option[DateTime] =
    getOpt[DateTime](columnIndex)
  def jodaDateTimeOpt(columnLabel: String): Option[DateTime] =
    getOpt[DateTime](columnLabel)

  def jodaLocalDateOpt(columnIndex: Int): Option[JodaLocalDate] =
    getOpt[JodaLocalDate](columnIndex)
  def jodaLocalDateOpt(columnLabel: String): Option[JodaLocalDate] =
    getOpt[JodaLocalDate](columnLabel)

  def jodaLocalTimeOpt(columnIndex: Int): Option[JodaLocalTime] =
    getOpt[JodaLocalTime](columnIndex)
  def jodaLocalTimeOpt(columnLabel: String): Option[JodaLocalTime] =
    getOpt[JodaLocalTime](columnLabel)

  def jodaLocalDateTimeOpt(columnIndex: Int): Option[JodaLocalDateTime] =
    getOpt[JodaLocalDateTime](columnIndex)
  def jodaLocalDateTimeOpt(columnLabel: String): Option[JodaLocalDateTime] =
    getOpt[JodaLocalDateTime](columnLabel)

  private[this] def get[A: TypeBinder](columnIndex: Int): A = {
    ensureCursor()
    wrapIfError(implicitly[TypeBinder[A]].apply(underlying, columnIndex))
  }

  private[this] def get[A: TypeBinder](columnLabel: String): A = {
    ensureCursor()
    wrapIfError(implicitly[TypeBinder[A]].apply(underlying, columnLabel))
  }

  private[this] def getOpt[A: TypeBinder](columnIndex: Int): Option[A] =
    get[Option[A]](columnIndex)

  private[this] def getOpt[A: TypeBinder](columnLabel: String): Option[A] =
    get[Option[A]](columnLabel)

}

object JodaWrappedResultSet {
  implicit def fromWrappedResultSetToJodaWrappedResultSet(
    rs: WrappedResultSet
  ): JodaWrappedResultSet =
    new JodaWrappedResultSet(rs.underlying, rs.cursor, rs.index)
}
