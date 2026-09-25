package unit

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.RegExpConstants.classNameRegExp

import scala.language.postfixOps

class ClassNameCamelToSnakeSpec extends AnyFlatSpec with Matchers {

  def newImplementation(className: String): String = {
    classNameRegExp.replaceAllIn(className, "")
  }

  def oldImplementation(className: String): String = {
    className
      .replaceFirst("\\$$", "")
      .replaceFirst("^.+\\.", "")
      .replaceFirst("^.+\\$", "")
  }

  behavior of "ClassName CamelToSpec"

  it should "match" in {
    val inputs = Set(
      "className",
      "prefix.className",
      "prefix$className",
      "className$",
      "prefix.className$",
      "prefix$className$",
      "className$postfix",
      "prefix.className$postfix",
      "prefix$className$postfix",
      "prefix..className$$postfix",
      "prefix$$className$$postfix",
      "additional.prefix.className$postfix",
      "additional$prefix$className$postfix",
      "additional.prefix.className$postfix$superfluous",
      "additional$prefix$className$postfix$superfluous",
    )

    inputs.map(name => {
      newImplementation(name) shouldBe oldImplementation(name)
    })
  }

}
