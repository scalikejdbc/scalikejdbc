package scalikejdbc

/**
 * BigInt converter.
 * @param value big integer value
 */
class ScalaBigIntegerConverter(val value: java.math.BigInteger) extends AnyVal {

  def toScalaBigInt: scala.math.BigInt = {
    if (value == null) null.asInstanceOf[scala.math.BigInt]
    else new scala.math.BigInt(value)
  }

}
