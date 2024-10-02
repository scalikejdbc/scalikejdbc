package foo

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.language.postfixOps
import scala.util.matching.Regex

class ClassNameCamelToSnakeSpec extends AnyFlatSpec with Matchers {

  // This regex removes trailing $, as well as anything until the first $ or .
  val classNameRegExp: Regex = "\\$$|^.*[.$](?=.+)".r

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
