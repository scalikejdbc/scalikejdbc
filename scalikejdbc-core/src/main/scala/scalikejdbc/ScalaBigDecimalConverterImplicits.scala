package scalikejdbc

import scala.language.implicitConversions

/**
 * Implicit conversions for BigDecimal values.
 */
trait ScalaBigDecimalConverterImplicits {

  implicit def convertBigDecimal(bd: java.math.BigDecimal): ScalaBigDecimalConverter = {
    new ScalaBigDecimalConverter(bd)
  }

}

/**
  * Implicit conversions for option BigDecimal values.
  */
trait ScalaBigDecimalConverterImplicits {

  implicit def convertBigDecimal(bd: java.math.BigDecimal): ScalaBigDecimalConverter = {
    new ScalaBigDecimalConverter(bd)
  }

  implicit def convertBigDecimalOpt(bd: Option[java.math.BigDecimal]): ScalaBigDecimalConverterOpt = {
    new ScalaBigDecimalConverterOpt(bd)
  }
}