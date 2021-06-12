package scalikejdbc.mapper

sealed abstract class GeneratorTemplate(val name: String)
  extends Product
  with Serializable

object GeneratorTemplate {
  case object interpolation extends GeneratorTemplate("interpolation")
  case object queryDsl extends GeneratorTemplate("queryDsl")

  private[this] val all = Set(interpolation, queryDsl)
  private[this] val map: Map[String, GeneratorTemplate] =
    all.map(t => t.name -> t).toMap

  def apply(name: String): GeneratorTemplate =
    map.getOrElse(name, sys.error(s"$name is not valid template name"))
}
