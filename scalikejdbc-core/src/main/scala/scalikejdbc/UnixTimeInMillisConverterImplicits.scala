package scalikejdbc

import scala.language.implicitConversions

import java.sql.{ Timestamp => sqlTimestamp, Time => sqlTime, Date => sqlDate }
import java.util.{ Date => utilDate }
import org.joda.time.LocalTime

/**
 * Implicit conversions for date time values.
 */
trait UnixTimeInMillisConverterImplicits {

  implicit def convertJavaUtilDateToConverter(t: utilDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlDateToConverter(t: sqlDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlTimeToConverter(t: sqlTime): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlTimestampToConverter(t: sqlTimestamp): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertLocalTimeToConverter(t: LocalTime): LocalTimeConverter = new LocalTimeConverter(t)

}

object UnixTimeInMillisConverterImplicits extends UnixTimeInMillisConverterImplicits
