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
package scalikejdbc.mapper

case class GeneratorConfig(srcDir: String = "src/main/scala",
  testDir: String = "src/test/scala",
  packageName: String = "models",
  template: GeneratorTemplate = GeneratorTemplate("queryDsl"),
  testTemplate: GeneratorTestTemplate = GeneratorTestTemplate(""),
  lineBreak: LineBreak = LineBreak("\n"),
  caseClassOnly: Boolean = false,
  encoding: String = "UTF-8",
  autoConstruct: Boolean = false,
  defaultAutoSession: Boolean = true,
  dateTimeClass: DateTimeClass = DateTimeClass.JodaDateTime,
  tableNameToClassName: String => String = GeneratorConfig.toCamelCase,
  columnNameToFieldName: String => String = GeneratorConfig.lowerCamelCase andThen GeneratorConfig.quoteReservedWord,
  returnCollectionType: ReturnCollectionType = ReturnCollectionType.List)

object GeneratorConfig {
  private def toProperCase(s: String): String = {
    import java.util.Locale.ENGLISH
    if (s == null || s.trim.size == 0) ""
    else s.substring(0, 1).toUpperCase(ENGLISH) + s.substring(1).toLowerCase(ENGLISH)
  }

  private val toCamelCase: String => String = _.split("_").foldLeft("") {
    (camelCaseString, part) =>
      camelCaseString + toProperCase(part)
  }

  val reservedWords: Set[String] = Set(
    "abstract", "case", "catch", "class", "def",
    "do", "else", "extends", "false", "final",
    "finally", "for", "forSome", "if", "implicit",
    "import", "lazy", "match", "new", "null", "macro",
    "object", "override", "package", "private", "protected",
    "return", "sealed", "super", "then", "this", "throw",
    "trait", "try", "true", "type", "val",
    "var", "while", "with", "yield"
  )

  val quoteReservedWord: String => String = {
    name =>
      if (reservedWords(name)) "`" + name + "`"
      else name
  }

  val lowerCamelCase: String => String =
    GeneratorConfig.toCamelCase.andThen {
      camelCase => camelCase.head.toLower + camelCase.tail
    }
}
