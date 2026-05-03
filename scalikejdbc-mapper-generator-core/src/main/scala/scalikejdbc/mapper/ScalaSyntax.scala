package scalikejdbc.mapper

sealed abstract class ScalaSyntax(private val value: String)
  extends Product
  with Serializable
object ScalaSyntax {
  case object New extends ScalaSyntax("new")
  case object Old extends ScalaSyntax("old")

  private val values: Seq[ScalaSyntax] = Seq(New, Old)

  def of(value: String): Option[ScalaSyntax] =
    values.find(_.value == value)
}
