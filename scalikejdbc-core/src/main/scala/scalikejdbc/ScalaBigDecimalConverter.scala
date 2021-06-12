package scalikejdbc

/**
 * BigDecimal converter.
 * @param value big decimal value
 */
class ScalaBigDecimalConverter(private val value: java.math.BigDecimal)
  extends AnyVal {

  def toScalaBigDecimal: scala.math.BigDecimal = {
    if (value == null) null.asInstanceOf[scala.math.BigDecimal]
    else new scala.math.BigDecimal(value)
  }

}
