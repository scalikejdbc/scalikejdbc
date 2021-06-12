package scalikejdbc

import scala.language.implicitConversions

/**
 * Implicit conversions for BigDecimal values.
 */
trait ScalaBigDecimalConverterImplicits {

  implicit def convertBigDecimal(
    bd: java.math.BigDecimal
  ): ScalaBigDecimalConverter = {
    new ScalaBigDecimalConverter(bd)
  }

}
