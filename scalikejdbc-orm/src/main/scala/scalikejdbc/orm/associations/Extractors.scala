package scalikejdbc.orm.associations

import scala.language.existentials
import scalikejdbc.orm._

/**
  * Extractor.
  *
  * @tparam Entity entity
  */
sealed trait Extractor[Entity]

/**
  * BelongsTo association extractor.
  *
  * @param mapper mapper
  * @param fk foreign key
  * @param alias table alias
  * @param merge function to merge associations
  * @param byDefault enable by default
  * @tparam Entity entity
  */
case class BelongsToExtractor[Entity](
  mapper: AssociationsFeature[?],
  fk: String,
  alias: Alias[Any],
  merge: (Entity, Option[Any]) => Entity,
  includesMerge: (Seq[Entity], Seq[?]) => Seq[Entity] =
    AssociationsFeature.defaultIncludesMerge[Entity, Any],
  var byDefault: Boolean = false
) extends Extractor[Entity]

/**
  * HasOne association extractor.
  *
  * @param mapper mapper
  * @param fk foreign key
  * @param alias table alias
  * @param merge function to merge associations
  * @param byDefault enable by default
  * @tparam Entity entity
  */
case class HasOneExtractor[Entity](
  mapper: AssociationsFeature[?],
  fk: String,
  alias: Alias[Any],
  merge: (Entity, Option[Any]) => Entity,
  includesMerge: (Seq[Entity], Seq[?]) => Seq[Entity] =
    AssociationsFeature.defaultIncludesMerge[Entity, Any],
  var byDefault: Boolean = false
) extends Extractor[Entity]

/**
  * HasMany association extractor.
  *
  * @param mapper mapper
  * @param fk foreign key
  * @param alias table alias
  * @param merge function to merge associations
  * @param byDefault enable by default
  * @tparam Entity entity
  */
case class HasManyExtractor[Entity](
  mapper: AssociationsFeature[?],
  fk: String,
  alias: Alias[Any],
  merge: (Entity, Seq[Any]) => Entity,
  includesMerge: (Seq[Entity], Seq[?]) => Seq[Entity] =
    AssociationsFeature.defaultIncludesMerge[Entity, Any],
  var byDefault: Boolean = false
) extends Extractor[Entity]
