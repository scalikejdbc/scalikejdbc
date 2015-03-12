package scalikejdbc.mapper

case class GeneratorTemplate(name: String)

object GeneratorTemplate {
  val interpolation = GeneratorTemplate("interpolation")
  val queryDsl = GeneratorTemplate("queryDsl")
}
