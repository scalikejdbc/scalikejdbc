/*
 * Copyright 2012 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * SQL Template Parser.
 *
 * This parser supports following templates.
 *
 * Basic SQL Template:
 *
 * {{{
 * select * from user where id = ? and user_name = ?
 * }}}
 *
 * Anorm-like SQL Template:
 *
 * {{{
 * select * from user where id = {id} and user_name = {userName}
 * }}}
 *
 * Executable SQL Template:
 * {{{
 * select * from user where id = /*'id*/123 and user_name = /*'userName*/\'Alice'
 * }}}
 *
 * `ExecutableSQL` is the template which contains parameter names just as comments with dummy values without specific syntax.
 * The template is a valid SQL, so you can check it is correct before building into app.
 */
object SQLTemplateParser extends JavaTokenParsers with LogSupport {

  /**
   * Extracts binding names from the SQL template.
   *
   * @param input input SQL
   * @return extracted parameter names
   */
  def extractAllParameters(input: String): List[Symbol] = {
    parse(mainParser, convertExecutableToAnorm(input)).getOrElse(Nil)
  }

  /**
   * Converts the SQL template to SQL template with place holders.
   *
   * @param input input SQL
   * @return simplified SQL
   */
  def convertToSQLWithPlaceHolders(input: String): String = {
    convertExecutableToAnorm(input).replaceAll("\\{.+?\\}", "?")
  }

  /**
   * Trims comments
   *
   * @param input SQL tempalte
   * @return SQL template without comments
   */
  def trimComments(input: String): String = ExecutableToAnormConverter(input).trimComments()

  /**
   * Converts Executable SQL template to Anorm SQL template.
   */
  private case class ExecutableToAnormConverter(str: String) {

    implicit def toStringWithMethodsInternally(sql: String) = ExecutableToAnormConverter(sql)

    def standardizeLineBreaks(): String = str.replaceAll("\r\n", "\n").replaceAll("\r", "\n")

    def trimSpaces() = {
      def trimSpaces(s: String, i: Int = 0): String = i match {
        case i if i > 10 => s
        case i => trimSpaces(s.replaceAll("  ", " "), i + 1)
      }
      trimSpaces(str).trim()
    }

    def removeLineComments() = str.split("\n").map(_.replaceFirst("--.+$", "")).mkString

    def removeMultipleLineComments() = str.replaceAll("/\\*\\s*.+?\\s*\\*/", "")

    def simplifyParameters() = str.replaceAll("/\\*\\s*'(\\w+)\\s*\\*/[^\\s,\\)]+", "{$1}")

    def trimParameterDummyValues() = {
      // because literals might have whitespace
      val paramComment = "(/\\*\\s*'.+?\\s*\\*/\\s*)"
      str.replaceAll(paramComment + "'[^']+'", "$1''")
        .replaceAll(paramComment + "\"[^\"]+\"", "$1\"\"")
    }

    def convert(): String = str.standardizeLineBreaks()
      .removeLineComments()
      .trimParameterDummyValues()
      .simplifyParameters()
      .removeMultipleLineComments()
      .trimSpaces()

    def trimComments(): String = str.standardizeLineBreaks()
      .removeLineComments()
      .removeMultipleLineComments()
      .trimSpaces()

  }

  private def convertExecutableToAnorm(input: String): String = ExecutableToAnormConverter(input).convert()

  // ----
  // Parser

  private def mainParser: Parser[List[Symbol]] = rep(name | other) ^^ {
    names => names.filter(_ != "").map(name => Symbol(name))
  }

  private def name = "\\{\\w+\\}".r <~ opt(",") ^^ {
    name =>
      name.replaceFirst("\\{", "").replaceFirst("\\}", "").trim()
  }

  private def other = literal | token ^^ (_ => "")

  private def literal = {
    def charLiteral = "'[^']*'".r ^^ (_ => "")
    // JavaTokenParsers stringLiteral does not work for string contains backslash, etc..
    def stringLiteral = ("\"[^(\")]*\"".r | super.stringLiteral) ^^ (_ => "")
    (stringLiteral | charLiteral | floatingPointNumber) ^^ (_ => "")
  }

  private def token = "[\\w\\(\\)\\.\\-\\+\\*/=,<>%;`]+".r ^^ (_ => "")

}
