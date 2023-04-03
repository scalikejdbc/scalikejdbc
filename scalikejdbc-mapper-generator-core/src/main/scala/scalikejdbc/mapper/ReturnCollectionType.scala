package scalikejdbc.mapper

sealed abstract class ReturnCollectionType(private val name: String)
  extends Product
  with Serializable

object ReturnCollectionType {
  case object List extends ReturnCollectionType("list")
  case object Vector extends ReturnCollectionType("vector")
  case object Array extends ReturnCollectionType("array")
  case object Factory extends ReturnCollectionType("factory")

  private[this] val all: Set[ReturnCollectionType] =
    Set(List, Vector, Array, Factory)
  private[scalikejdbc] val map: Map[String, ReturnCollectionType] =
    all.map(c => c.name -> c).toMap
}
