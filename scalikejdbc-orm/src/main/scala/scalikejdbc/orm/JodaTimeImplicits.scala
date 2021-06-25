package scalikejdbc.orm

import scalikejdbc.jodatime.{ JodaBinders, JodaWrappedResultSet }
import scalikejdbc.{ ParameterBinderFactory, TypeBinder, WrappedResultSet }

import scala.language.implicitConversions

trait JodaTimeImplicits {

  // Note: Since ScalikeJDBC 3.2, joda-time implicits are not enabled by default
  implicit val jodaDateTimeParameterBinderFactory
    : ParameterBinderFactory[org.joda.time.DateTime] =
    JodaBinders.jodaDateTime
  implicit val jodaLocalDateTimeParameterBinderFactory
    : ParameterBinderFactory[org.joda.time.LocalDateTime] =
    JodaBinders.jodaLocalDateTime
  implicit val jodaLocalDateParameterBinderFactory
    : ParameterBinderFactory[org.joda.time.LocalDate] =
    JodaBinders.jodaLocalDate
  implicit val jodaLocalTimeParameterBinderFactory
    : ParameterBinderFactory[org.joda.time.LocalTime] =
    JodaBinders.jodaLocalTime

  import org.joda.time.{
    DateTime => JodaDateTime,
    LocalDate => JodaLocalDate,
    LocalDateTime => JodaLocalDateTime,
    LocalTime => JodaLocalTime
  }

  implicit val jodaDateTimeTypeBinder: TypeBinder[JodaDateTime] =
    JodaBinders.jodaDateTime
  implicit val jodaLocalDateTypeBinder: TypeBinder[JodaLocalDate] =
    JodaBinders.jodaLocalDate
  implicit val jodaLocalTimeTypeBinder: TypeBinder[JodaLocalTime] =
    JodaBinders.jodaLocalTime
  implicit val jodaLocalDateTimeTypeBinder: TypeBinder[JodaLocalDateTime] =
    JodaBinders.jodaLocalDateTime

  // Keep rs.jodaDateTime method calls compatible in 2.6
  implicit def fromWrappedResultSetToJodaWrappedResultSet(
    rs: WrappedResultSet
  ): JodaWrappedResultSet =
    new JodaWrappedResultSet(rs.underlying, rs.cursor, rs.index)

}

object JodaTimeImplicits extends JodaTimeImplicits
