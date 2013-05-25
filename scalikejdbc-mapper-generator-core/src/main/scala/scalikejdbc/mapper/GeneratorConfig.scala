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
  encoding: String = "UTF-8")

object GeneratorTemplate {

  val basic = GeneratorTemplate("basic")
  val namedParameters = GeneratorTemplate("namedParameters")
  val executable = GeneratorTemplate("executable")
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

