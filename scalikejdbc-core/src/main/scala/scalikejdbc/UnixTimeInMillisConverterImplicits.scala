package scalikejdbc

import scala.language.implicitConversions
import java.sql.{ Date => sqlDate, Time => sqlTime, Timestamp => sqlTimestamp }
import java.util.{ Date => utilDate }

import scalikejdbc.jodatime.LocalTimeConverter

/**
 * Implicit conversions for date time values.
 */
trait UnixTimeInMillisConverterImplicits {

  implicit def convertJavaUtilDateToConverter(t: utilDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlDateToConverter(t: sqlDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlTimeToConverter(t: sqlTime): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlTimestampToConverter(t: sqlTimestamp): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertLocalTimeToConverter(t: org.joda.time.LocalTime): LocalTimeConverter = new LocalTimeConverter(t)

}

object UnixTimeInMillisConverterImplicits extends UnixTimeInMillisConverterImplicits
