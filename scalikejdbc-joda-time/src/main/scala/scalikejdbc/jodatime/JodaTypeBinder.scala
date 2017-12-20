package scalikejdbc
package jodatime

import org.joda.time.{ DateTime => JodaDateTime }
import org.joda.time.{ LocalDate => JodaLocalDate }
import org.joda.time.{ LocalTime => JodaLocalTime }
import org.joda.time.{ LocalDateTime => JodaLocalDateTime }

/**
 * Type binder for java.sql.ResultSet.
 */
object JodaTypeBinder {

  implicit val jodaDateTimeTypeBinder: TypeBinder[JodaDateTime] = JodaBinders.jodaDateTime
  implicit val jodaLocalDateTypeBinder: TypeBinder[JodaLocalDate] = JodaBinders.jodaLocalDate
  implicit val jodaLocalTimeTypeBinder: TypeBinder[JodaLocalTime] = JodaBinders.jodaLocalTime
  implicit val jodaLocalDateTimeTypeBinder: TypeBinder[JodaLocalDateTime] = JodaBinders.jodaLocalDateTime

}
