package scalikejdbc.mapper

object GeneratorTestTemplate {
  val ScalaTestFlatSpec: GeneratorTestTemplate = GeneratorTestTemplate(
    "ScalaTestFlatSpec"
  )
  val specs2unit: GeneratorTestTemplate = GeneratorTestTemplate("specs2unit")
  val specs2acceptance: GeneratorTestTemplate = GeneratorTestTemplate(
    "specs2acceptance"
  )
}

case class GeneratorTestTemplate(name: String)
