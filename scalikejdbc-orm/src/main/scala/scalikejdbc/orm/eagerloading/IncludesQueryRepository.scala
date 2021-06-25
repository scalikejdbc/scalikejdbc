package scalikejdbc.orm.eagerloading

import scalikejdbc.orm.associations.{ BelongsToExtractor, HasOneExtractor }
import scalikejdbc.orm.associations.HasManyExtractor

import scala.collection.mutable

/**
  * Entity repository for includes queries.
  *
  * @tparam Entity base entity type
  */
trait IncludesQueryRepository[Entity] {

  private[this] val belongsTo: mutable.Map[BelongsToExtractor[Entity], Seq[?]] =
    new scala.collection.concurrent.TrieMap()
  private[this] val hasOne: mutable.Map[HasOneExtractor[Entity], Seq[?]] =
    new scala.collection.concurrent.TrieMap()
  private[this] val hasMany: mutable.Map[HasManyExtractor[Entity], Seq[?]] =
    new scala.collection.concurrent.TrieMap()

  /**
    * Returns entities for belongsTo relation.
    *
    * @param extractor extractor
    * @return entities
    */
  def entitiesFor(extractor: BelongsToExtractor[Entity]): Seq[?] =
    belongsTo.getOrElse(extractor, Nil)

  /**
    * Returns entities for hasOne relation.
    *
    * @param extractor extractor
    * @return entities
    */
  def entitiesFor(extractor: HasOneExtractor[Entity]): Seq[?] =
    hasOne.getOrElse(extractor, Nil)

  /**
    * Returns entities for hasMany relation.
    *
    * @param extractor extractor
    * @return entities
    */
  def entitiesFor(extractor: HasManyExtractor[Entity]): Seq[?] =
    hasMany.getOrElse(extractor, Nil)

  /**
    * Put an entity to repository.
    *
    * @param extractor extractor
    * @param entity entity
    */
  def putAndReturn[A](extractor: BelongsToExtractor[Entity], entity: A): A = {
    belongsTo.update(extractor, belongsTo.getOrElse(extractor, Nil).+:(entity))
    entity
  }

  /**
    * Put an entity to repository.
    *
    * @param extractor extractor
    * @param entity entity
    */
  def putAndReturn[A](extractor: HasOneExtractor[Entity], entity: A): A = {
    hasOne.update(extractor, hasOne.getOrElse(extractor, Nil).+:(entity))
    entity
  }

  /**
    * Put an entity to repository.
    *
    * @param extractor extractor
    * @param entity entity
    */
  def putAndReturn[A](extractor: HasManyExtractor[Entity], entity: A): A = {
    hasMany.update(extractor, hasMany.getOrElse(extractor, Nil).+:(entity))
    entity
  }
}

/**
  * IncludesQueryRepository factory.
  */
object IncludesQueryRepository {

  def apply[Entity](): IncludesQueryRepository[Entity] =
    new DefaultIncludesQueryRepository[Entity]
}
