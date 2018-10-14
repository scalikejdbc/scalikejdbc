package scalikejdbc

import scala.language.implicitConversions
import java.sql.{ Date => sqlDate, Time => sqlTime, Timestamp => sqlTimestamp }
import java.util.{ Date => utilDate }

/**
 * Implicit conversions for date time values.
 */
@deprecated("use JavaUtilDateConverterImplicits", "3.3.2")
trait UnixTimeInMillisConverterImplicits {

  @deprecated("use JavaUtilDateConverterImplicits.toJavaUtilDateConverter", "3.3.2")
  def convertJavaUtilDateToConverter(t: utilDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  @deprecated("use JavaUtilDateConverterImplicits.toJavaUtilDateConverter", "3.3.2")
  def convertJavaSqlDateToConverter(t: sqlDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  @deprecated("use JavaUtilDateConverterImplicits.toJavaUtilDateConverter", "3.3.2")
  def convertJavaSqlTimeToConverter(t: sqlTime): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  @deprecated("use JavaUtilDateConverterImplicits.toJavaUtilDateConverter", "3.3.2")
  def convertJavaSqlTimestampToConverter(t: sqlTimestamp): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

}

@deprecated("use JavaUtilDateConverterImplicits", "3.3.2")
object UnixTimeInMillisConverterImplicits extends UnixTimeInMillisConverterImplicits
