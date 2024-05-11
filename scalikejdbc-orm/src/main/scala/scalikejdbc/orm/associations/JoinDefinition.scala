package scalikejdbc.orm.associations

import scalikejdbc.orm._
import scalikejdbc._
import org.slf4j.LoggerFactory

/**
  * Join definition.
  *
  * @param joinType join type (innerJoin/LeftOuterJoin)
  * @param thisMapper this mapper
  * @param leftMapper left mapper
  * @param leftAlias left table alias
  * @param rightMapper right mapper
  * @param rightAlias right table alias
  * @param on join condition
  * @param fk foreign key to join
  * @param enabledEvenIfAssociated enable even if the right one is associated
  * @param enabledByDefault enable by default
  * @tparam Entity entity
  */
case class JoinDefinition[Entity](
  joinType: JoinType,
  thisMapper: AssociationsFeature[Entity],
  leftMapper: AssociationsFeature[Any],
  leftAlias: Alias[Any],
  rightMapper: AssociationsFeature[Any],
  rightAlias: Alias[Any],
  on: SQLSyntax,
  fk: Option[(Any) => Option[Long]] = None,
  var enabledEvenIfAssociated: Boolean = false,
  var enabledByDefault: Boolean = false
) {

  private[this] val logger =
    LoggerFactory.getLogger(classOf[JoinDefinition[Entity]])

  /**
    * Enables by default even if the right one is associated to others.
    *
    * @return join definition
    */
  def byDefaultEvenIfAssociated(): JoinDefinition[Entity] = byDefault(true)

  /**
    * Enables by default.
    *
    * @param enabledEvenIfAssociated even if associated
    * @return join definition
    */
  def byDefault(
    enabledEvenIfAssociated: Boolean = true
  ): JoinDefinition[Entity] = {
    val isDefaultAlias = thisMapper.defaultAlias == this.rightAlias
    val alreadyExistsYet = thisMapper.defaultJoinDefinitions.contains(this)
    val alreadySameNameExistsYetForOtherEntity = !alreadyExistsYet &&
      thisMapper.defaultJoinDefinitions
        .map(_.rightAlias.tableAliasName)
        .contains(this.rightAlias.tableAliasName)

    if (isDefaultAlias) {
      logger.debug(
        s"Skipped this name '${this.rightAlias}' is the default alias of this mapper. (joinDef:${this})"
      )
    } else if (alreadyExistsYet) {
      logger.debug(
        s"Skipped appending to the default join definitions because this join definition already exists. (joinDef:${this})"
      )
    } else if (alreadySameNameExistsYetForOtherEntity) {
      logger.warn(
        s"Skipped because same name '${this.rightAlias}' is already used by another definition. You need to use different alias. (joinDef:${this})"
      )
    } else {
      this.enabledByDefault = true
      this.enabledEvenIfAssociated = enabledEvenIfAssociated
      thisMapper.defaultJoinDefinitions.add(this)
    }
    this
  }

}
