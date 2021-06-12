package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ClassNameUtilSpec extends AnyFlatSpec with Matchers {

  object Foo {
    object Bar
  }

  behavior of "ClassNameUtil"

  it should "return class name" in {
    ClassNameUtil.getClassName("string".getClass) should equal(
      "java.lang.String"
    )
    ClassNameUtil.getClassName(Array("hoge").getClass) should equal(
      "java.lang.String[]"
    )
    ClassNameUtil.getClassName(Foo.Bar.getClass) shouldBe a[String]
  }
}
