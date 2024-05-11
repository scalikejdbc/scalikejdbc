package scalikejdbc.orm.associations

import scala.language.existentials

import scala.collection.mutable

/**
  * Association.
  *
  * @tparam Entity entity
  */
sealed trait Association[Entity] {

  /**
    * ORM mapper instance.
    */
  def mapper: AssociationsFeature[Entity]

  /**
    * Join definitions.
    */
  def joinDefinitions: mutable.LinkedHashSet[JoinDefinition[?]]

  /**
    * Enables extractor by default.
    */
  def setExtractorByDefault(): Unit

  /**
    * Activates this association by default.
    */
  def byDefault: Association[Entity] = {
    joinDefinitions.foreach { joinDef =>
      joinDef.byDefault(joinDef.enabledEvenIfAssociated)
      mapper.defaultJoinDefinitions.add(joinDef)
    }
    setExtractorByDefault()
    mapper.associations.add(this)
    this
  }

}

/**
  * BelongsTo relation.
  *
  * @param mapper mapper
  * @param joinDefinitions join definitions
  * @param extractor extractor
  * @tparam Entity entity
  */
case class BelongsToAssociation[Entity](
  mapper: AssociationsFeature[Entity],
  joinDefinitions: mutable.LinkedHashSet[JoinDefinition[?]],
  extractor: BelongsToExtractor[Entity]
) extends Association[Entity] {

  override def setExtractorByDefault() = mapper.setAsByDefault(extractor)

  def includes[A](
    merge: (Seq[Entity], Seq[A]) => Seq[Entity]
  ): BelongsToAssociation[Entity] = {
    this.copy(extractor =
      extractor.copy(includesMerge =
        merge.asInstanceOf[(Seq[Entity], Seq[?]) => Seq[Entity]]
      )
    )
  }
}

/**
  * HasOne association.
  *
  * @param mapper mapper
  * @param joinDefinitions join definitions
  * @param extractor extractor
  * @tparam Entity entity
  */
case class HasOneAssociation[Entity](
  mapper: AssociationsFeature[Entity],
  joinDefinitions: mutable.LinkedHashSet[JoinDefinition[?]],
  extractor: HasOneExtractor[Entity]
) extends Association[Entity] {

  override def setExtractorByDefault() = mapper.setAsByDefault(extractor)

  def includes[A](
    merge: (Seq[Entity], Seq[A]) => Seq[Entity]
  ): HasOneAssociation[Entity] = {
    this.copy(extractor =
      extractor.copy(includesMerge =
        merge.asInstanceOf[(Seq[Entity], Seq[?]) => Seq[Entity]]
      )
    )
  }
}

/**
  * HasMany association.
  *
  * @param mapper mapper
  * @param joinDefinitions join definitions
  * @param extractor extractor
  * @tparam Entity entity
  */
case class HasManyAssociation[Entity](
  mapper: AssociationsFeature[Entity],
  joinDefinitions: mutable.LinkedHashSet[JoinDefinition[?]],
  extractor: HasManyExtractor[Entity]
) extends Association[Entity] {

  override def setExtractorByDefault() = mapper.setAsByDefault(extractor)

  def includes[A](
    merge: (Seq[Entity], Seq[A]) => Seq[Entity]
  ): HasManyAssociation[Entity] = {
    this.copy(extractor =
      extractor.copy(includesMerge =
        merge.asInstanceOf[(Seq[Entity], Seq[?]) => Seq[Entity]]
      )
    )
  }
}
