import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GeneratorConfigSpec extends AnyFlatSpec with Matchers {

  "A GeneratorConfig" should "chain a given name to a proper camelCaseCaps format" in {
    import scalikejdbc.mapper.GeneratorConfig.toCamelCaseCaps
    toCamelCaseCaps("abc") should be("Abc")
    toCamelCaseCaps("abc_def") should be("AbcDef")
    toCamelCaseCaps("abc_123") should be("Abc_123")
    toCamelCaseCaps("abc_123a") should be("Abc_123a")
    toCamelCaseCaps("abc_1_2_3_a") should be("Abc_1_2_3A")
    toCamelCaseCaps("abc_1_2abc_3_a") should be("Abc_1_2abc_3A")
    toCamelCaseCaps("abc_1_2abc_3_a45_678") should be("Abc_1_2abc_3A45_678")
  }
}
