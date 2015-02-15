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
  tableNameToClassName: String => String = GeneratorConfig.toCamelCase)

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
}

sealed abstract class DateTimeClass(private[scalikejdbc] val name: String) extends Product with Serializable {
  private[scalikejdbc] val simpleName = name.split('.').last
}
object DateTimeClass {
  case object JodaDateTime extends DateTimeClass("org.joda.time.DateTime")
  case object ZonedDateTime extends DateTimeClass("java.time.ZonedDateTime")
  case object OffsetDateTime extends DateTimeClass("java.time.OffsetDateTime")

  private[scalikejdbc] val all = Set(
    JodaDateTime, ZonedDateTime, OffsetDateTime
  )

  private[scalikejdbc] val map: Map[String, DateTimeClass] =
    all.map(clazz => clazz.name -> clazz).toMap
}

object GeneratorTemplate {
  val interpolation = GeneratorTemplate("interpolation")
  val queryDsl = GeneratorTemplate("queryDsl")
}

case class GeneratorTemplate(name: String)

object GeneratorTestTemplate {
  val ScalaTestFlatSpec = GeneratorTestTemplate("ScalaTestFlatSpec")
  val specs2unit = GeneratorTestTemplate("specs2unit")
  val specs2acceptance = GeneratorTestTemplate("specs2acceptance")
}
case class GeneratorTestTemplate(name: String)

object LineBreak {
  def value(name: String) = name match {
    case "CR" => "\r"
    case "LF" => "\n"
    case "CRLF" => "\r\n"
    case _ => "\n"
  }
}

case class LineBreak(name: String) {
  def value = LineBreak.value(name)
}

