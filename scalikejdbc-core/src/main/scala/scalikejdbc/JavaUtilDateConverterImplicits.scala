package scalikejdbc

import scala.language.implicitConversions

/**
 * Implicit conversions for date time values.
 */
trait JavaUtilDateConverterImplicits {
  implicit def toJavaUtilDateConverter(
    value: java.util.Date
  ): JavaUtilDateConverter =
    new JavaUtilDateConverter(value)
}

object JavaUtilDateConverterImplicits extends JavaUtilDateConverterImplicits
