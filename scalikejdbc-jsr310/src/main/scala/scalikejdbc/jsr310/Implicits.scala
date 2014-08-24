package scalikejdbc.jsr310

import java.util.{ GregorianCalendar, Calendar }

import scala.language.implicitConversions
import java.time._
import scalikejdbc.{ TypeBinder, WrappedResultSet }

object Implicits extends Implicits

trait Implicits {

  import TypeBinder._

  implicit def fromWrappedResultSetToJSR310WrappedResultSet(rs: WrappedResultSet): JSR310WrappedResultSet =
    new JSR310WrappedResultSet(rs)

  implicit val zonedDateTime: TypeBinder[ZonedDateTime] = option[java.sql.Timestamp].map(_.map(v => ZonedDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())).orNull[ZonedDateTime])
  implicit val offsetDateTime: TypeBinder[OffsetDateTime] = option[java.sql.Timestamp].map(_.map(v => OffsetDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())).orNull[OffsetDateTime])
  implicit val localDate: TypeBinder[LocalDate] = option[java.sql.Date].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDate).orNull[LocalDate])
  implicit val localTime: TypeBinder[LocalTime] = option[java.sql.Time].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalTime).orNull[LocalTime])
  implicit val localDateTime: TypeBinder[LocalDateTime] = option[java.sql.Timestamp].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDateTime).orNull[LocalDateTime])

  implicit val zonedDateTimeOpt: TypeBinder[Option[ZonedDateTime]] = option[java.sql.Timestamp].map(_.map(v => ZonedDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())))
  implicit val offsetDateTimeOpt: TypeBinder[Option[OffsetDateTime]] = option[java.sql.Timestamp].map(_.map(v => OffsetDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())))
  implicit val localDateOpt: TypeBinder[Option[LocalDate]] = option[java.sql.Date].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDate))
  implicit val localTimeOpt: TypeBinder[Option[LocalTime]] = option[java.sql.Time].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalTime))
  implicit val localDateTimeOpt: TypeBinder[Option[LocalDateTime]] = option[java.sql.Timestamp].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDateTime))

}
