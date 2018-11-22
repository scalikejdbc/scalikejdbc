package scalikejdbc

import org.scalatest.{ FlatSpec, Matchers }

class ClassNameUtilSpec extends FlatSpec with Matchers {

  object Foo {
    object Bar
  }

  behavior of "ClassNameUtil"

  it should "return class name" in {
    ClassNameUtil.getClassName("string".getClass) should equal("java.lang.String")
    ClassNameUtil.getClassName(Array("hoge").getClass) should equal("java.lang.String[]")
    ClassNameUtil.getClassName(Foo.Bar.getClass) shouldBe a[String]
  }
}
