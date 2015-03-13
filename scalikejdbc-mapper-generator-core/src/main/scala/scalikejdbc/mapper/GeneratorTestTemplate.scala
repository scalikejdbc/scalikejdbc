package scalikejdbc.mapper

object GeneratorTestTemplate {
  val ScalaTestFlatSpec = GeneratorTestTemplate("ScalaTestFlatSpec")
  val specs2unit = GeneratorTestTemplate("specs2unit")
  val specs2acceptance = GeneratorTestTemplate("specs2acceptance")
}

case class GeneratorTestTemplate(name: String)
