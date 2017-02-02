package scalikejdbc

/**
 * BigDecimal converter.
 * @param value big decimal value
 */
class ScalaBigDecimalConverter(val value: java.math.BigDecimal) extends AnyVal {

  def toScalaBigDecimal: scala.math.BigDecimal = {
    if (value == null) null.asInstanceOf[scala.math.BigDecimal]
    else new scala.math.BigDecimal(value)
  }

}

/**
 * BigDecimal Option converter.
 * @param value option big decimal value
 */
class ScalaBigDecimalConverterOpt(val value: Option[java.math.BigDecimal]) extends AnyVal {
  def toScalaBigDecimalOpt(x: Option[java.math.BigDecimal]) = x match {
    case None => None
    case Some(x) => Option(new BigDecimal(x))
  }
}
