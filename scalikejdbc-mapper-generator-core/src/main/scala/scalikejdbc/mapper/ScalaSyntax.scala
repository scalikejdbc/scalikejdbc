package scalikejdbc.mapper

sealed abstract class ScalaSyntax(private val value: String)
  extends Product
  with Serializable
object ScalaSyntax {
  case object Scala3 extends ScalaSyntax("scala3")
  case object New extends ScalaSyntax("new")
  case object Old extends ScalaSyntax("old")

  private val values: Seq[ScalaSyntax] = Seq(Scala3, New, Old)

  def of(value: String): Option[ScalaSyntax] =
    values.find(_.value == value)
}
