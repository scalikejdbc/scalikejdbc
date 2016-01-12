package scalikejdbc

import scala.language.implicitConversions

/**
 * Implicit conversions for BigInteger values.
 */
trait ScalaBigIntegerConverterImplicits {

  implicit def convertBigInteger(bi: java.math.BigInteger): ScalaBigIntegerConverter = {
    new ScalaBigIntegerConverter(bi)
  }

}
