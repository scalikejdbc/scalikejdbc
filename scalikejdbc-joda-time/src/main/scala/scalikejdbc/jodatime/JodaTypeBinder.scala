package scalikejdbc
package jodatime

import org.joda.time.{ DateTime => JodaDateTime }
import org.joda.time.{ LocalDate => JodaLocalDate }
import org.joda.time.{ LocalTime => JodaLocalTime }
import org.joda.time.{ LocalDateTime => JodaLocalDateTime }
import JodaUnixTimeInMillisConverterImplicits._

/**
 * Type binder for java.sql.ResultSet.
 */
object JodaTypeBinder extends JodaTypeBinderInstances1 {
  implicit def jodaDateTimeTypeBinder(implicit
    z: OverwrittenZoneId
  ): TypeBinder[JodaDateTime] =
    Binders.utilDate.map(
      Binders.nullThrough(_.toJodaDateTimeWithZoneId(z.value))
    )
  implicit def jodaLocalDateTypeBinder(implicit
    z: OverwrittenZoneId
  ): TypeBinder[JodaLocalDate] =
    Binders.sqlDate.map(
      Binders.nullThrough(_.toJodaLocalDateWithZoneId(z.value))
    )
  implicit def jodaLocalTimeTypeBinder(implicit
    z: OverwrittenZoneId
  ): TypeBinder[JodaLocalTime] =
    Binders.sqlTime.map(
      Binders.nullThrough(_.toJodaLocalTimeWithZoneId(z.value))
    )
  implicit def jodaLocalDateTimeTypeBinder(implicit
    z: OverwrittenZoneId
  ): TypeBinder[JodaLocalDateTime] =
    Binders.utilDate.map(
      Binders.nullThrough(_.toJodaLocalDateTimeWithZoneId(z.value))
    )
}

sealed abstract class JodaTypeBinderInstances1 {
  implicit val jodaDateTimeTypeBinderDefault: TypeBinder[JodaDateTime] =
    JodaBinders.jodaDateTime
  implicit val jodaLocalDateTypeBinderDefault: TypeBinder[JodaLocalDate] =
    JodaBinders.jodaLocalDate
  implicit val jodaLocalTimeTypeBinderDefault: TypeBinder[JodaLocalTime] =
    JodaBinders.jodaLocalTime
  implicit val jodaLocalDateTimeTypeBinderDefault
    : TypeBinder[JodaLocalDateTime] = JodaBinders.jodaLocalDateTime
}
