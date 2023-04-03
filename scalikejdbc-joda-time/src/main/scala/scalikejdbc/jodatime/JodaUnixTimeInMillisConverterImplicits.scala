package scalikejdbc
package jodatime

import scala.language.implicitConversions
import java.sql.{ Date => sqlDate, Time => sqlTime, Timestamp => sqlTimestamp }
import java.util.{ Date => utilDate }

/**
 * Implicit conversions for date time values.
 */
trait JodaUnixTimeInMillisConverterImplicits {

  implicit def convertJavaUtilDateToJodaConverter(
    t: utilDate
  ): JodaUnixTimeInMillisConverter = new JodaUnixTimeInMillisConverter(
    t.getTime
  )

  implicit def convertJavaSqlDateToJodaConverter(
    t: sqlDate
  ): JodaUnixTimeInMillisConverter = new JodaUnixTimeInMillisConverter(
    t.getTime
  )

  implicit def convertJavaSqlTimeToJodaConverter(
    t: sqlTime
  ): JodaUnixTimeInMillisConverter = new JodaUnixTimeInMillisConverter(
    t.getTime
  )

  implicit def convertJavaSqlTimestampToJodaConverter(
    t: sqlTimestamp
  ): JodaUnixTimeInMillisConverter = new JodaUnixTimeInMillisConverter(
    t.getTime
  )

  implicit def convertLocalTimeToJodaConverter(
    t: org.joda.time.LocalTime
  ): scalikejdbc.jodatime.LocalTimeConverter = new LocalTimeConverter(t)
}

object JodaUnixTimeInMillisConverterImplicits
  extends JodaUnixTimeInMillisConverterImplicits
