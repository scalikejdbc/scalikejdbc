package scalikejdbc

import org.scalatest.{FlatSpec, Matchers}


class ScalaBigDecimalConverterOptSpec extends FlatSpec with Matchers {

  behavior of "ScalaBigDecimalConverterOpt"

  it should "return None when passing in None" in {
    new ScalaBigDecimalConverterOpt(None).toScalaBigDecimalOpt should be (None)
  }

  it should "return Option[scala.math.BigDecimal] 1 when passing in Option[java.math.BigDecimal] 1" in {
    new ScalaBigDecimalConverterOpt(Option(new java.math.BigDecimal(1))).toScalaBigDecimalOpt should be (Option(scala.math.BigDecimal(1)))
  }
}
