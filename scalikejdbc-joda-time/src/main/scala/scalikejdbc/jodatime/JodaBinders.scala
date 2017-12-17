package scalikejdbc
package jodatime

import Binders._
import JodaUnixTimeInMillisConverterImplicits._

object JodaBinders {

  val jodaDateTime: Binders[org.joda.time.DateTime] =
    utilDate.xmap(nullThrough(_.toJodaDateTime), _.toDate)
  val jodaLocalDateTime: Binders[org.joda.time.LocalDateTime] =
    utilDate.xmap(nullThrough(_.toJodaLocalDateTime), _.toDate)
  val jodaLocalDate: Binders[org.joda.time.LocalDate] =
    sqlDate.xmap(nullThrough(_.toJodaLocalDate), _.toDate.toSqlDate)
  val jodaLocalTime: Binders[org.joda.time.LocalTime] =
    sqlTime.xmap(nullThrough(_.toJodaLocalTime), _.toSqlTime)

}
