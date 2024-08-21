package scalikejdbc.orm.associations

// Don't change this import
import scalikejdbc._
import scalikejdbc.orm.Alias
import scalikejdbc.orm.basic.{
  AutoSessionFeature,
  ConnectionPoolFeature,
  IdFeature,
  SQLSyntaxSupportBase
}
import scalikejdbc.orm.eagerloading.IncludesQueryRepository
import scalikejdbc.orm.exception.AssociationSettingsException
import scalikejdbc.orm.internals.JavaReflectionUtil
import scalikejdbc.orm.logging.LoggerProvider

import scala.collection.mutable
import scala.language.existentials
import scala.util.Try

object AssociationsFeature {

  def defaultIncludesMerge[Entity, A]: (Seq[Entity], Seq[A]) => Seq[Entity] =
    (_, _) =>
      throw new AssociationSettingsException(
        """
          |--------- Invalid Association Settings ---------
          |
          |  Merge function for includes query is required.
          |
          |  e.g.
          |
          |  val company = belongsTo[Company](Company, (e, c) => e.copy(company = c))
          |    .includes[Company]((es, cs) => es.map { e =>
          |      cs.find(c => e.exists(_.id == c.id)).map(v => e.copy(company = Some(v))).getOrElse(e)
          |    }
          |
          |-------------------------------------------------
          |""".stripMargin
      )

}

/**
 * Associations support feature which has Id.
 * @tparam Id id
 * @tparam Entity entity
 */
trait AssociationsWithIdFeature[Id, Entity]
  extends AssociationsFeature[Entity]
  with IdFeature[Id]

/**
 * Associations support feature.
 *
 * @tparam Entity entity
 */
trait AssociationsFeature[Entity]
  extends SQLSyntaxSupportBase[Entity]
  with ConnectionPoolFeature
  with AutoSessionFeature
  with LoggerProvider { self: SQLSyntaxSupport[Entity] =>

  import AssociationsFeature._

  /**
   * Associations
   */
  val associations = new mutable.LinkedHashSet[Association[?]]

  private[scalikejdbc] def belongsToAssociations
    : Seq[BelongsToAssociation[Entity]] = {
    associations.collect { case x: BelongsToAssociation[Entity] @unchecked =>
      x
    }.toSeq
  }
  private[scalikejdbc] def hasOneAssociations
    : Seq[HasOneAssociation[Entity]] = {
    associations.collect { case x: HasOneAssociation[Entity] @unchecked =>
      x
    }.toSeq
  }
  private[scalikejdbc] def hasManyAssociations
    : Seq[HasManyAssociation[Entity]] = {
    associations.collect { case x: HasManyAssociation[Entity] @unchecked =>
      x
    }.toSeq
  }

  /**
   * Join definitions that are enabled by default.
   */
  val defaultJoinDefinitions = new mutable.LinkedHashSet[JoinDefinition[?]]()

  private def unshiftJoinDefinition(
    newOne: JoinDefinition[?],
    definitions: mutable.LinkedHashSet[JoinDefinition[?]]
  ): mutable.LinkedHashSet[JoinDefinition[?]] = {
    val newDefinitions = new mutable.LinkedHashSet[JoinDefinition[?]]()
    newDefinitions.add(newOne)
    newDefinitions ++= definitions
  }

  // ----------------------
  // Join Definition
  // ----------------------

  /**
   * Creates a new join definition.
   *
   * @param joinType join type
   * @param left left mapper and table alias
   * @param right right mapper and table alias
   * @param on join condition
   * @return join definition
   */
  def createJoinDefinition[Left, Right](
    joinType: JoinType,
    left: (AssociationsFeature[?], Alias[Left]),
    right: (AssociationsFeature[?], Alias[Right]),
    on: SQLSyntax
  ): JoinDefinition[Entity] = {
    val (leftMapper, leftAlias) = left
    val (rightMapper, rightAlias) = right
    JoinDefinition[Entity](
      joinType,
      this,
      leftMapper.asInstanceOf[AssociationsFeature[Any]],
      leftAlias.asInstanceOf[Alias[Any]],
      rightMapper.asInstanceOf[AssociationsFeature[Any]],
      rightAlias.asInstanceOf[Alias[Any]],
      on
    )
  }

  // ----------------------
  // Inner Join Definition
  // ----------------------

  // using default alias

  def joinWithDefaults(
    right: AssociationsFeature[?],
    on: SQLSyntax
  ): JoinDefinition[Entity] = {
    innerJoinWithDefaults(right, on)
  }
  def joinWithDefaults(
    right: AssociationsFeature[?],
    on: (Alias[Entity], Alias[Any]) => SQLSyntax
  ): JoinDefinition[Entity] = {
    innerJoinWithDefaults(right, on)
  }
  def joinWithDefaults[Left](
    left: AssociationsFeature[Left],
    right: AssociationsFeature[Entity],
    on: (Alias[Left], Alias[Entity]) => SQLSyntax
  ): JoinDefinition[Entity] = {
    innerJoinWithDefaults[Left](left, right, on)
  }

  def innerJoinWithDefaults(
    right: AssociationsFeature[?],
    on: SQLSyntax
  ): JoinDefinition[Entity] = {
    createJoinDefinition(
      InnerJoin,
      this -> this.defaultAlias,
      right -> right.defaultAlias,
      on
    )
  }
  def innerJoinWithDefaults(
    right: AssociationsFeature[?],
    on: (Alias[Entity], Alias[Any]) => SQLSyntax
  ): JoinDefinition[Entity] = {
    createJoinDefinition(
      InnerJoin,
      this -> this.defaultAlias,
      right -> right.defaultAlias,
      on.apply(this.defaultAlias, right.defaultAlias.asInstanceOf[Alias[Any]])
    )
  }
  def innerJoinWithDefaults[Left](
    left: AssociationsFeature[Left],
    right: AssociationsFeature[Entity],
    on: (Alias[Left], Alias[Entity]) => SQLSyntax
  ): JoinDefinition[Entity] = {
    createJoinDefinition(
      InnerJoin,
      left -> left.defaultAlias,
      right -> right.defaultAlias,
      on.apply(left.defaultAlias, right.defaultAlias)
    )
  }

  // using specified alias

  def join[Right](
    right: (AssociationsFeature[Right], Alias[Right]),
    on: (Alias[Entity], Alias[Right]) => SQLSyntax
  ): JoinDefinition[Entity] = {
    innerJoin(right, on)
  }
  def join[Left](
    left: (AssociationsFeature[Left], Alias[Left]),
    right: (AssociationsFeature[Entity], Alias[Entity]),
    on: (Alias[Left], Alias[Entity]) => SQLSyntax
  ): JoinDefinition[Entity] = {
    innerJoin(left, right, on)
  }
  def innerJoin[Right](
    right: (AssociationsFeature[Right], Alias[Right]),
    on: (Alias[Entity], Alias[Right]) => SQLSyntax
  ): JoinDefinition[Entity] = {
    createJoinDefinition(
      InnerJoin,
      this -> this.defaultAlias,
      right,
      on.apply(this.defaultAlias, right._2)
    )
  }
  def innerJoin[Left](
    left: (AssociationsFeature[Left], Alias[Left]),
    right: (AssociationsFeature[Entity], Alias[Entity]),
    on: (Alias[Left], Alias[Entity]) => SQLSyntax
  ): JoinDefinition[Entity] = {
    createJoinDefinition(InnerJoin, left, right, on.apply(left._2, right._2))
  }

  // ----------------------
  // Left Outer Join Definitions
  // ----------------------

  // using default alias

  def leftJoinWithDefaults(
    right: AssociationsFeature[?],
    on: SQLSyntax
  ): JoinDefinition[?] = {
    createJoinDefinition(
      LeftOuterJoin,
      this -> this.defaultAlias,
      right -> right.defaultAlias,
      on
    )
  }
  def leftJoinWithDefaults(
    right: AssociationsFeature[?],
    on: (Alias[Entity], Alias[Any]) => SQLSyntax
  ): JoinDefinition[?] = {
    createJoinDefinition(
      LeftOuterJoin,
      this -> this.defaultAlias,
      right -> right.defaultAlias,
      on.apply(this.defaultAlias, right.defaultAlias.asInstanceOf[Alias[Any]])
    )
  }
  def leftJoinWithDefaults[Left, Right](
    left: AssociationsFeature[Left],
    right: AssociationsFeature[Right],
    on: (Alias[Left], Alias[Right]) => SQLSyntax
  ): JoinDefinition[?] = {
    createJoinDefinition(
      LeftOuterJoin,
      left -> left.defaultAlias,
      right -> right.defaultAlias,
      on.apply(left.defaultAlias, right.defaultAlias)
    )
  }

  // using specified alias

  def leftJoin[Right](
    right: (AssociationsFeature[Right], Alias[Right]),
    on: (Alias[Entity], Alias[Right]) => SQLSyntax
  ): JoinDefinition[?] = {
    createJoinDefinition(
      LeftOuterJoin,
      this -> this.defaultAlias,
      right,
      on.apply(this.defaultAlias, right._2)
    )
  }
  def leftJoin[Left, Right](
    left: (AssociationsFeature[?], Alias[Left]),
    right: (AssociationsFeature[?], Alias[Right]),
    on: (Alias[Left], Alias[Right]) => SQLSyntax
  ): JoinDefinition[?] = {
    createJoinDefinition(
      LeftOuterJoin,
      left,
      right,
      on.apply(left._2, right._2)
    )
  }

  // ----------------------
  // One-to-one
  // ----------------------

  // belongs-to

  def setAsByDefault(extractor: BelongsToExtractor[Entity]): Unit = {
    extractor.byDefault = true
    defaultBelongsToExtractors.add(extractor)
  }

  def belongsTo[A](
    right: AssociationsWithIdFeature[?, A],
    merge: (Entity, Option[A]) => Entity
  ): BelongsToAssociation[Entity] = {
    val fk = toDefaultForeignKeyName[A](right)
    belongsToWithJoinCondition[A](
      right,
      sqls.eq(
        this.defaultAlias.field(fk),
        right.defaultAlias.field(right.primaryKeyFieldName)
      ),
      merge
    )
  }

  def belongsToWithJoinCondition[A](
    right: AssociationsWithIdFeature[?, A],
    on: SQLSyntax,
    merge: (Entity, Option[A]) => Entity
  ): BelongsToAssociation[Entity] = {
    val joinDef = leftJoinWithDefaults(right, on)
    val extractor = extractBelongsTo[A](
      right,
      toDefaultForeignKeyName[A](right),
      right.defaultAlias,
      merge
    )
    new BelongsToAssociation[Entity](
      this,
      unshiftJoinDefinition(
        joinDef,
        right.defaultJoinDefinitions.filter(_.enabledEvenIfAssociated)
      ),
      extractor
    )
  }

  def belongsToWithFk[A](
    right: AssociationsWithIdFeature[?, A],
    fk: String,
    merge: (Entity, Option[A]) => Entity
  ): BelongsToAssociation[Entity] = {
    belongsToWithFkAndJoinCondition(
      right,
      fk,
      sqls.eq(
        this.defaultAlias.field(fk),
        right.defaultAlias.field(right.primaryKeyFieldName)
      ),
      merge
    )
  }

  def belongsToWithFkAndJoinCondition[A](
    right: AssociationsFeature[A],
    fk: String,
    on: SQLSyntax,
    merge: (Entity, Option[A]) => Entity
  ): BelongsToAssociation[Entity] = {
    val joinDef = leftJoinWithDefaults(right, on)
    val extractor = extractBelongsTo[A](right, fk, right.defaultAlias, merge)
    new BelongsToAssociation[Entity](
      this,
      unshiftJoinDefinition(
        joinDef,
        right.defaultJoinDefinitions.filter(_.enabledEvenIfAssociated)
      ),
      extractor
    )
  }

  def belongsToWithAlias[A](
    right: (AssociationsWithIdFeature[?, A], Alias[A]),
    merge: (Entity, Option[A]) => Entity
  ): BelongsToAssociation[Entity] = {
    val fk = if (right._1.defaultAlias != right._2) {
      val fieldName = right._1.primaryKeyFieldName
      val primaryKeyFieldName =
        fieldName.head.toString.toUpperCase + fieldName.tail
      right._2.tableAliasName + primaryKeyFieldName
    } else {
      toDefaultForeignKeyName[A](right._1)
    }
    belongsToWithAliasAndFk(right, fk, merge)
  }

  def belongsToWithAliasAndFk[A](
    right: (AssociationsWithIdFeature[?, A], Alias[A]),
    fk: String,
    merge: (Entity, Option[A]) => Entity
  ): BelongsToAssociation[Entity] = {
    belongsToWithAliasAndFkAndJoinCondition(
      right,
      fk,
      sqls.eq(
        this.defaultAlias.field(fk),
        right._2.field(right._1.primaryKeyFieldName)
      ),
      merge
    )
  }

  def belongsToWithAliasAndFkAndJoinCondition[A](
    right: (AssociationsFeature[A], Alias[A]),
    fk: String,
    on: SQLSyntax,
    merge: (Entity, Option[A]) => Entity
  ): BelongsToAssociation[Entity] = {
    val joinDef =
      createJoinDefinition(LeftOuterJoin, this -> this.defaultAlias, right, on)
    val extractor = extractBelongsTo[A](right._1, fk, right._2, merge)
    new BelongsToAssociation[Entity](
      this,
      unshiftJoinDefinition(
        joinDef,
        right._1.defaultJoinDefinitions.filter(_.enabledEvenIfAssociated)
      ),
      extractor
    )
  }

  // has-one

  def setAsByDefault(extractor: HasOneExtractor[Entity]): Unit = {
    extractor.byDefault = true
    defaultHasOneExtractors.add(extractor)
  }

  def hasOne[A](
    right: AssociationsFeature[A],
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {
    hasOneWithFk[A](right, toDefaultForeignKeyName[Entity](this), merge)
  }

  def hasOneWithJoinCondition[A](
    right: AssociationsFeature[A],
    on: SQLSyntax,
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {
    hasOneWithFkAndJoinCondition(
      right,
      toDefaultForeignKeyName[Entity](this),
      on,
      merge
    )
  }

  def hasOneWithFk[A](
    right: AssociationsFeature[A],
    fk: String,
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {
    hasOneWithFkAndJoinCondition(
      right,
      fk,
      sqls.eq(
        this.defaultAlias.field(this.primaryKeyFieldName),
        right.defaultAlias.field(fk)
      ),
      merge
    )
  }

  def hasOneWithFkAndJoinCondition[A](
    right: AssociationsFeature[A],
    fk: String,
    on: SQLSyntax,
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {
    val joinDef = leftJoinWithDefaults(right, on)
    val extractor = extractHasOne[A](right, fk, right.defaultAlias, merge)
    new HasOneAssociation[Entity](
      this,
      unshiftJoinDefinition(
        joinDef,
        right.defaultJoinDefinitions.filter(_.enabledEvenIfAssociated)
      ),
      extractor
    )
  }

  def hasOneWithAlias[A](
    right: (AssociationsFeature[A], Alias[A]),
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {
    hasOneWithAliasAndFk(right, toDefaultForeignKeyName[Entity](this), merge)
  }

  def hasOneWithAliasAndJoinCondition[A](
    right: (AssociationsFeature[A], Alias[A]),
    on: SQLSyntax,
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {
    hasOneWithAliasAndFkAndJoinCondition(
      right,
      toDefaultForeignKeyName[Entity](this),
      on,
      merge
    )
  }

  def hasOneWithAliasAndFk[A](
    right: (AssociationsFeature[A], Alias[A]),
    fk: String,
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {
    hasOneWithAliasAndFkAndJoinCondition(
      right,
      fk,
      sqls.eq(
        this.defaultAlias.field(this.primaryKeyFieldName),
        right._2.field(fk)
      ),
      merge
    )
  }

  def hasOneWithAliasAndFkAndJoinCondition[A](
    right: (AssociationsFeature[A], Alias[A]),
    fk: String,
    on: SQLSyntax,
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {
    val joinDef =
      createJoinDefinition(LeftOuterJoin, this -> this.defaultAlias, right, on)
    val extractor = extractHasOne[A](right._1, fk, right._2, merge)
    new HasOneAssociation[Entity](
      this,
      unshiftJoinDefinition(
        joinDef,
        right._1.defaultJoinDefinitions.filter(_.enabledEvenIfAssociated)
      ),
      extractor
    )
  }

  // ----------------------
  // One-to-many
  // ----------------------

  // has-many

  def setAsByDefault(extractor: HasManyExtractor[Entity]): Unit = {
    extractor.byDefault = true
    defaultOneToManyExtractors.add(extractor)
  }

  def hasMany[M](
    many: (AssociationsFeature[M], Alias[M]),
    on: (Alias[Entity], Alias[M]) => SQLSyntax,
    merge: (Entity, Seq[M]) => Entity
  ): HasManyAssociation[Entity] = {
    val fkOnManySide: String = {
      if (Try(many._1.primaryKeyFieldName).isFailure)
        toDefaultForeignKeyName(this)
      else many._1.primaryKeyFieldName
    }
    hasManyWithFk(many, fkOnManySide, on, merge)
  }

  def hasManyWithFk[M](
    many: (AssociationsFeature[M], Alias[M]),
    fk: String,
    on: (Alias[Entity], Alias[M]) => SQLSyntax,
    merge: (Entity, Seq[M]) => Entity
  ): HasManyAssociation[Entity] = {

    val joinDef = leftJoin(
      this -> this.defaultAlias,
      many,
      on
    )
    val extractor = extractOneToMany[M](
      mapper = many._1,
      alias = many._2,
      fk = fk,
      merge = merge
    )
    val definitions =
      new mutable.LinkedHashSet().+=(joinDef).++(many._1.defaultJoinDefinitions)
    new HasManyAssociation[Entity](this, definitions, extractor)
  }

  def hasManyThrough[M2](
    through: AssociationsFeature[?],
    many: AssociationsWithIdFeature[?, M2],
    merge: (Entity, Seq[M2]) => Entity
  ): HasManyAssociation[Entity] = {

    val throughFk = toDefaultForeignKeyName[Entity](this)
    val manyFk = toDefaultForeignKeyName[M2](many)
    hasManyThrough(
      through =
        through.asInstanceOf[AssociationsFeature[Any]] -> through.defaultAlias
          .asInstanceOf[Alias[Any]],
      throughOn = (entity, m1: Alias[Any]) =>
        sqls.eq(entity.field(primaryKeyFieldName), m1.field(throughFk)),
      many = many -> many.defaultAlias,
      on = (m1: Alias[Any], m2: Alias[M2]) =>
        sqls.eq(m1.field(manyFk), m2.field(many.primaryKeyFieldName)),
      merge = merge
    )
  }

  def hasManyThroughWithFk[M2](
    through: AssociationsFeature[?],
    many: AssociationsWithIdFeature[?, M2],
    throughFk: String,
    manyFk: String,
    merge: (Entity, Seq[M2]) => Entity
  ): HasManyAssociation[Entity] = {

    hasManyThrough(
      through =
        through.asInstanceOf[AssociationsFeature[Any]] -> through.defaultAlias
          .asInstanceOf[Alias[Any]],
      throughOn = (entity, m1: Alias[Any]) =>
        sqls.eq(entity.field(primaryKeyFieldName), m1.field(throughFk)),
      many = many -> many.defaultAlias,
      on = (m1: Alias[Any], m2: Alias[M2]) =>
        sqls.eq(m1.field(manyFk), m2.field(many.primaryKeyFieldName)),
      merge = merge
    )
  }

  def hasManyThrough[M1, M2](
    through: (AssociationsFeature[M1], Alias[M1]),
    throughOn: (Alias[Entity], Alias[M1]) => SQLSyntax,
    many: (AssociationsWithIdFeature[?, M2], Alias[M2]),
    on: (Alias[M1], Alias[M2]) => SQLSyntax,
    merge: (Entity, Seq[M2]) => Entity
  ): HasManyAssociation[Entity] = {

    val joinDef1 = leftJoin(
      through,
      throughOn
    )
    val joinDef2 = leftJoin(
      through,
      many,
      on
    )
    val definitions = new mutable.LinkedHashSet()
      .+=(joinDef1, joinDef2)
      .++(many._1.defaultJoinDefinitions)
    val extractor =
      extractOneToMany[M2](many._1, many._1.primaryKeyFieldName, many._2, merge)
    new HasManyAssociation[Entity](this, definitions, extractor)
  }

  // ----------------------
  // Query Builder
  // ----------------------

  /**
   * Returns a select query builder that all associations are joined.
   *
   * @param sql sql object
   * @param belongsToAssociations belongsTo associations
   * @param hasOneAssociations hasOne associations
   * @param hasManyAssociations hasMany associations
   * @return select query builder
   */
  def selectQueryWithAdditionalAssociations(
    sql: SelectSQLBuilder[Entity],
    belongsToAssociations: Seq[BelongsToAssociation[Entity]],
    hasOneAssociations: Seq[HasOneAssociation[Entity]],
    hasManyAssociations: Seq[HasManyAssociation[Entity]]
  ): SelectSQLBuilder[Entity] = {

    val mergedJoinDefinitions =
      (belongsToAssociations.flatMap(_.joinDefinitions)
        ++ hasOneAssociations.flatMap(_.joinDefinitions)
        ++ hasManyAssociations.flatMap(_.joinDefinitions))
        .filterNot { df =>
          val currentName = df.rightAlias.tableAliasName
          val sameAsThis = this.defaultAlias.tableAliasName == currentName
          val foundInDefaults = defaultJoinDefinitions.exists(d =>
            d.rightAlias.tableAliasName == currentName
          )
          foundInDefaults || sameAsThis
        }
        .foldLeft(mutable.LinkedHashSet[JoinDefinition[?]]()) { (dfs, df) =>
          val currentName = df.rightAlias.tableAliasName
          val duplicated =
            dfs.exists(d => d.rightAlias.tableAliasName == currentName)
          if (duplicated) dfs else dfs + df
        }

    mergedJoinDefinitions.foldLeft(sql) { (query, join) =>
      // Merge soft deletion or something else default scope condition here
      // (Left one must have the defaultScope in where clause)
      val condition: SQLSyntax = sqls
        .toAndConditionOpt(
          Some(join.on),
          join.rightMapper.defaultScope(join.rightAlias)
        )
        .get

      join.joinType match {
        case InnerJoin =>
          query.innerJoin(join.rightMapper.as(join.rightAlias)).on(condition)
        case LeftOuterJoin =>
          query.leftJoin(join.rightMapper.as(join.rightAlias)).on(condition)
        case jt => throw new IllegalStateException(s"Unexpected pattern ${jt}")
      }
    }
  }

  /**
   * Returns the default select query builder for this mapper.
   *
   * @return select query builder
   */
  override def defaultSelectQuery: SelectSQLBuilder[Entity] = {
    buildDefaultJoins(super.defaultSelectQuery)
  }

  /**
   * Returns the count query builder for this mapper.
   *
   * @return select query builder
   */
  override def simpleCountQuery: SelectSQLBuilder[Entity] = {
    buildDefaultJoins(super.simpleCountQuery)
  }

  private[this] def buildDefaultJoins(
    selectQuery: SelectSQLBuilder[Entity]
  ): SelectSQLBuilder[Entity] = {
    // Notice: LinkedHashSet because elements in the order they were inserted
    val definitions = defaultJoinDefinitions.foldLeft(
      mutable.LinkedHashSet[JoinDefinition[?]]()
    ) { (dfs, df) =>
      val currentName = df.rightAlias.tableAliasName
      val duplicated =
        dfs.exists(d => d.rightAlias.tableAliasName == currentName)
      if (duplicated) dfs else dfs + df
    }
    definitions.foldLeft(selectQuery) { (query, join) =>
      join.joinType match {
        case InnerJoin if join.enabledByDefault =>
          query.innerJoin(join.rightMapper.as(join.rightAlias)).on(join.on)
        case LeftOuterJoin if join.enabledByDefault =>
          query.leftJoin(join.rightMapper.as(join.rightAlias)).on(join.on)
        case _ => query
      }
    }
  }

  // ----------------------
  // ResultSet Extractor
  // ----------------------

  private[this] def extractHasMany(
    ex: HasManyExtractor[Entity],
    rs: WrappedResultSet
  )(implicit
    includesRepository: IncludesQueryRepository[Entity]
  ): Option[Entity] = {
    if (rs.anyOpt(ex.alias.resultName.field(ex.fk)).isDefined) {
      val mapper = ex.mapper.asInstanceOf[AssociationsFeature[Any]]
      val alias = ex.alias.asInstanceOf[Alias[Any]]
      Some(
        includesRepository
          .putAndReturn(ex, mapper.extract(rs, alias.resultName))
          .asInstanceOf[Entity]
      )
    } else None
  }

  def extract(sql: SQL[Entity, NoExtractor])(implicit
    includesRepository: IncludesQueryRepository[Entity] =
      IncludesQueryRepository[Entity]()
  ): SQL[Entity, HasExtractor] = {
    extractWithAssociations(
      sql,
      belongsToAssociations,
      hasOneAssociations,
      hasManyAssociations
    )
  }

  /**
   * Creates an extractor for this query.
   *
   * @param sql sql object
   * @param belongsToAssociations belongsTo associations
   * @param hasOneAssociations hasOne associations
   * @param oneToManyAssociations hasMany associations
   * @return sql object
   */
  def extractWithAssociations(
    sql: SQL[Entity, NoExtractor],
    belongsToAssociations: Seq[BelongsToAssociation[Entity]],
    hasOneAssociations: Seq[HasOneAssociation[Entity]],
    oneToManyAssociations: Seq[HasManyAssociation[Entity]]
  )(implicit
    includesRepository: IncludesQueryRepository[Entity] =
      IncludesQueryRepository[Entity]()
  ): SQL[Entity, HasExtractor] = {

    val enabledJoinDefinitions = defaultJoinDefinitions ++
      belongsToAssociations.map(_.joinDefinitions) ++
      hasOneAssociations.map(_.joinDefinitions) ++
      oneToManyAssociations.map(_.joinDefinitions)

    val enabledOneToManyExtractors =
      defaultOneToManyExtractors ++ oneToManyAssociations.map(_.extractor)

    if (enabledJoinDefinitions.isEmpty) {
      sql.map(rs => extract(rs, defaultAlias.resultName))

    } else if (enabledOneToManyExtractors.size > 0) {
      val oneExtractedSql: OneToXSQL[Entity, NoExtractor, Entity] =
        sql.one(rs =>
          extractWithOneToOneTables(
            rs,
            belongsToAssociations.map(_.extractor).toSet,
            hasOneAssociations.map(_.extractor).toSet
          )
        )

      if (enabledOneToManyExtractors.size == 1) {
        // one-to-many
        val ex: HasManyExtractor[Entity] = enabledOneToManyExtractors.head
        val sql: OneToManySQL[Entity, Entity, HasExtractor, Entity] =
          oneExtractedSql
            .toMany { rs =>
              extractHasMany(ex, rs)
            }
            .map {
              /*case*/
              (one, many) =>
                ex.merge(one, many.toIndexedSeq)
            }
        sql

      } else if (enabledOneToManyExtractors.size == 2) {
        // one-to-manies 2
        val Seq(ex1: HasManyExtractor[Entity], ex2: HasManyExtractor[Entity]) =
          enabledOneToManyExtractors.toSeq
        val sql: OneToManies2SQL[Entity, ?, ?, HasExtractor, Entity] =
          oneExtractedSql
            .toManies(
              to1 = rs => extractHasMany(ex1, rs),
              to2 = rs => extractHasMany(ex2, rs)
            )
            .map {
              /*case*/
              (one, m1, m2) =>
                ex2.merge(ex1.merge(one, m1.toIndexedSeq), m2.toIndexedSeq)
            }
        sql

      } else if (enabledOneToManyExtractors.size == 3) {
        // one-to-manies 3
        val Seq(
          ex1: HasManyExtractor[Entity],
          ex2: HasManyExtractor[Entity],
          ex3: HasManyExtractor[Entity]
        ) =
          enabledOneToManyExtractors.toSeq
        val sql: OneToManies3SQL[Entity, ?, ?, ?, HasExtractor, Entity] =
          oneExtractedSql
            .toManies(
              to1 = rs => extractHasMany(ex1, rs),
              to2 = rs => extractHasMany(ex2, rs),
              to3 = rs => extractHasMany(ex3, rs)
            )
            .map {
              /*case*/
              (one, m1, m2, m3) =>
                ex3.merge(
                  ex2.merge(ex1.merge(one, m1.toIndexedSeq), m2.toIndexedSeq),
                  m3.toIndexedSeq
                )
            }
        sql

      } else if (enabledOneToManyExtractors.size == 4) {
        // one-to-manies 4
        val Seq(
          ex1: HasManyExtractor[Entity],
          ex2: HasManyExtractor[Entity],
          ex3: HasManyExtractor[Entity],
          ex4: HasManyExtractor[Entity]
        ) = enabledOneToManyExtractors.toSeq
        val sql: OneToManies4SQL[Entity, ?, ?, ?, ?, HasExtractor, Entity] =
          oneExtractedSql
            .toManies(
              to1 = rs => extractHasMany(ex1, rs),
              to2 = rs => extractHasMany(ex2, rs),
              to3 = rs => extractHasMany(ex3, rs),
              to4 = rs => extractHasMany(ex4, rs)
            )
            .map {
              /*case*/
              (one, m1, m2, m3, m4) =>
                ex4.merge(
                  ex3.merge(
                    ex2.merge(ex1.merge(one, m1.toIndexedSeq), m2.toIndexedSeq),
                    m3.toIndexedSeq
                  ),
                  m4.toIndexedSeq
                )
            }
        sql

      } else if (enabledOneToManyExtractors.size == 5) {
        // one-to-manies 5
        val Seq(
          ex1: HasManyExtractor[Entity],
          ex2: HasManyExtractor[Entity],
          ex3: HasManyExtractor[Entity],
          ex4: HasManyExtractor[Entity],
          ex5: HasManyExtractor[Entity]
        ) = enabledOneToManyExtractors.toSeq
        val sql: OneToManies5SQL[Entity, ?, ?, ?, ?, ?, HasExtractor, Entity] =
          oneExtractedSql
            .toManies(
              to1 = rs => extractHasMany(ex1, rs),
              to2 = rs => extractHasMany(ex2, rs),
              to3 = rs => extractHasMany(ex3, rs),
              to4 = rs => extractHasMany(ex4, rs),
              to5 = rs => extractHasMany(ex5, rs)
            )
            .map {
              /*case*/
              (one, m1, m2, m3, m4, m5) =>
                ex5.merge(
                  ex4.merge(
                    ex3.merge(
                      ex2.merge(
                        ex1.merge(one, m1.toIndexedSeq),
                        m2.toIndexedSeq
                      ),
                      m3.toIndexedSeq
                    ),
                    m4.toIndexedSeq
                  ),
                  m5.toIndexedSeq
                )
            }
        sql

      } else if (enabledOneToManyExtractors.size == 6) {
        // one-to-manies 6
        val Seq(
          ex1: HasManyExtractor[Entity],
          ex2: HasManyExtractor[Entity],
          ex3: HasManyExtractor[Entity],
          ex4: HasManyExtractor[Entity],
          ex5: HasManyExtractor[Entity],
          ex6: HasManyExtractor[Entity]
        ) = enabledOneToManyExtractors.toSeq
        val sql
          : OneToManies6SQL[Entity, ?, ?, ?, ?, ?, ?, HasExtractor, Entity] =
          oneExtractedSql
            .toManies(
              to1 = rs => extractHasMany(ex1, rs),
              to2 = rs => extractHasMany(ex2, rs),
              to3 = rs => extractHasMany(ex3, rs),
              to4 = rs => extractHasMany(ex4, rs),
              to5 = rs => extractHasMany(ex5, rs),
              to6 = rs => extractHasMany(ex6, rs)
            )
            .map {
              /*case*/
              (one, m1, m2, m3, m4, m5, m6) =>
                ex6.merge(
                  ex5.merge(
                    ex4.merge(
                      ex3.merge(
                        ex2.merge(
                          ex1.merge(one, m1.toIndexedSeq),
                          m2.toIndexedSeq
                        ),
                        m3.toIndexedSeq
                      ),
                      m4.toIndexedSeq
                    ),
                    m5.toIndexedSeq
                  ),
                  m6.toIndexedSeq
                )
            }
        sql

      } else if (enabledOneToManyExtractors.size == 7) {
        // one-to-manies 7
        val Seq(
          ex1: HasManyExtractor[Entity],
          ex2: HasManyExtractor[Entity],
          ex3: HasManyExtractor[Entity],
          ex4: HasManyExtractor[Entity],
          ex5: HasManyExtractor[Entity],
          ex6: HasManyExtractor[Entity],
          ex7: HasManyExtractor[Entity]
        ) = enabledOneToManyExtractors.toSeq
        val sql
          : OneToManies7SQL[Entity, ?, ?, ?, ?, ?, ?, ?, HasExtractor, Entity] =
          oneExtractedSql
            .toManies(
              to1 = rs => extractHasMany(ex1, rs),
              to2 = rs => extractHasMany(ex2, rs),
              to3 = rs => extractHasMany(ex3, rs),
              to4 = rs => extractHasMany(ex4, rs),
              to5 = rs => extractHasMany(ex5, rs),
              to6 = rs => extractHasMany(ex6, rs),
              to7 = rs => extractHasMany(ex7, rs)
            )
            .map {
              /*case*/
              (one, m1, m2, m3, m4, m5, m6, m7) =>
                ex7.merge(
                  ex6.merge(
                    ex5.merge(
                      ex4.merge(
                        ex3.merge(
                          ex2.merge(
                            ex1.merge(one, m1.toIndexedSeq),
                            m2.toIndexedSeq
                          ),
                          m3.toIndexedSeq
                        ),
                        m4.toIndexedSeq
                      ),
                      m5.toIndexedSeq
                    ),
                    m6.toIndexedSeq
                  ),
                  m7.toIndexedSeq
                )
            }
        sql

      } else if (enabledOneToManyExtractors.size == 8) {
        // one-to-manies 8
        val Seq(
          ex1: HasManyExtractor[Entity],
          ex2: HasManyExtractor[Entity],
          ex3: HasManyExtractor[Entity],
          ex4: HasManyExtractor[Entity],
          ex5: HasManyExtractor[Entity],
          ex6: HasManyExtractor[Entity],
          ex7: HasManyExtractor[Entity],
          ex8: HasManyExtractor[Entity]
        ) = enabledOneToManyExtractors.toSeq
        val sql: OneToManies8SQL[
          Entity,
          ?,
          ?,
          ?,
          ?,
          ?,
          ?,
          ?,
          ?,
          HasExtractor,
          Entity
        ] = oneExtractedSql
          .toManies(
            to1 = rs => extractHasMany(ex1, rs),
            to2 = rs => extractHasMany(ex2, rs),
            to3 = rs => extractHasMany(ex3, rs),
            to4 = rs => extractHasMany(ex4, rs),
            to5 = rs => extractHasMany(ex5, rs),
            to6 = rs => extractHasMany(ex6, rs),
            to7 = rs => extractHasMany(ex7, rs),
            to8 = rs => extractHasMany(ex8, rs)
          )
          .map {
            /*case*/
            (one, m1, m2, m3, m4, m5, m6, m7, m8) =>
              ex8.merge(
                ex7.merge(
                  ex6.merge(
                    ex5.merge(
                      ex4.merge(
                        ex3.merge(
                          ex2.merge(
                            ex1.merge(one, m1.toIndexedSeq),
                            m2.toIndexedSeq
                          ),
                          m3.toIndexedSeq
                        ),
                        m4.toIndexedSeq
                      ),
                      m5.toIndexedSeq
                    ),
                    m6.toIndexedSeq
                  ),
                  m7.toIndexedSeq
                ),
                m8.toIndexedSeq
              )
          }
        sql

      } else if (enabledOneToManyExtractors.size == 9) {
        // one-to-manies 9
        val Seq(
          ex1: HasManyExtractor[Entity],
          ex2: HasManyExtractor[Entity],
          ex3: HasManyExtractor[Entity],
          ex4: HasManyExtractor[Entity],
          ex5: HasManyExtractor[Entity],
          ex6: HasManyExtractor[Entity],
          ex7: HasManyExtractor[Entity],
          ex8: HasManyExtractor[Entity],
          ex9: HasManyExtractor[Entity]
        ) = enabledOneToManyExtractors.toSeq
        val sql: OneToManies9SQL[
          Entity,
          ?,
          ?,
          ?,
          ?,
          ?,
          ?,
          ?,
          ?,
          ?,
          HasExtractor,
          Entity
        ] = oneExtractedSql
          .toManies(
            to1 = rs => extractHasMany(ex1, rs),
            to2 = rs => extractHasMany(ex2, rs),
            to3 = rs => extractHasMany(ex3, rs),
            to4 = rs => extractHasMany(ex4, rs),
            to5 = rs => extractHasMany(ex5, rs),
            to6 = rs => extractHasMany(ex6, rs),
            to7 = rs => extractHasMany(ex7, rs),
            to8 = rs => extractHasMany(ex8, rs),
            to9 = rs => extractHasMany(ex9, rs)
          )
          .map {
            /*case*/
            (one, m1, m2, m3, m4, m5, m6, m7, m8, m9) =>
              ex9.merge(
                ex8.merge(
                  ex7.merge(
                    ex6.merge(
                      ex5.merge(
                        ex4.merge(
                          ex3.merge(
                            ex2.merge(
                              ex1.merge(one, m1.toIndexedSeq),
                              m2.toIndexedSeq
                            ),
                            m3.toIndexedSeq
                          ),
                          m4.toIndexedSeq
                        ),
                        m5.toIndexedSeq
                      ),
                      m6.toIndexedSeq
                    ),
                    m7.toIndexedSeq
                  ),
                  m8.toIndexedSeq
                ),
                m9.toIndexedSeq
              )
          }
        sql

      } else {
        throw new IllegalStateException(
          s"Unsupported one-to-manies settings. (max: 9, actual: ${defaultOneToManyExtractors.size})"
        )
      }

    } else {
      // several one-to-one and so on
      sql.map(rs =>
        extractWithOneToOneTables(
          rs,
          belongsToAssociations.map(_.extractor).toSet,
          hasOneAssociations.map(_.extractor).toSet
        )
      )
    }
  }

  /**
   * Extracts entity with one-to-one tables.
   *
   * @param rs result set
   * @param belongsToExtractors belongsTo extractors
   * @param hasOneExtractors hasOne extractors
   * @return entity
   */
  def extractWithOneToOneTables(
    rs: WrappedResultSet,
    belongsToExtractors: Set[BelongsToExtractor[Entity]],
    hasOneExtractors: Set[HasOneExtractor[Entity]]
  )(implicit includesRepository: IncludesQueryRepository[Entity]): Entity = {

    val allBelongsTo = defaultBelongsToExtractors ++ belongsToExtractors
    val withBelongsTo =
      allBelongsTo.foldLeft(extract(rs, defaultAlias.resultName)) {
        case (entity, extractor) =>
          val mapper = extractor.mapper.asInstanceOf[AssociationsFeature[Any]]
          val toOne: Option[?] = rs
            .anyOpt(defaultAlias.resultName.field(extractor.fk))
            .flatMap { _ =>
              try {
                val entity = mapper.extract(
                  rs,
                  extractor.alias.resultName.asInstanceOf[ResultName[Any]]
                )
                Some(includesRepository.putAndReturn(extractor, entity))
              } catch {
                case e: ResultSetExtractorException =>
                  // Although fk in the left entity is available
                  // but the right entity is absent when the right one is deleted softly
                  logger.debug(
                    s"The right entity is absent. It may be deleted softly. (fk: ${extractor.fk})"
                  )
                  None
              }
            }
          extractor.merge(entity, toOne)
      }
    val allHasOne = defaultHasOneExtractors ++ hasOneExtractors
    val withAssociations = allHasOne.foldLeft(withBelongsTo) {
      case (entity, extractor) =>
        val mapper = extractor.mapper.asInstanceOf[AssociationsFeature[Any]]
        val toOne: Option[?] = rs
          .anyOpt(extractor.alias.resultName.field(extractor.fk))
          .flatMap { _ =>
            try {
              val entity = mapper.extract(
                rs,
                extractor.alias.resultName.asInstanceOf[ResultName[Any]]
              )
              Some(includesRepository.putAndReturn(extractor, entity))
            } catch {
              case e: ResultSetExtractorException =>
                // Although fk in the left entity is available
                // but the right entity is absent when the right one is deleted softly
                logger.debug(
                  s"The right entity is absent. It may be deleted softly. (fk: ${extractor.fk})"
                )
                None
            }
          }
        extractor.merge(entity, toOne)
    }
    withAssociations
  }

  // -----------------------------------------
  // One to One Relation
  // -----------------------------------------

  val defaultBelongsToExtractors =
    new mutable.LinkedHashSet[BelongsToExtractor[Entity]]()

  def extractBelongsTo[That](
    mapper: AssociationsFeature[That],
    fk: String,
    alias: Alias[That],
    merge: (Entity, Option[That]) => Entity,
    includesMerge: (Seq[Entity], Seq[That]) => Seq[Entity] =
      defaultIncludesMerge[Entity, That]
  ): BelongsToExtractor[Entity] = {
    BelongsToExtractor[Entity](
      mapper,
      fk,
      alias.asInstanceOf[Alias[Any]],
      merge.asInstanceOf[(Entity, Option[Any]) => Entity],
      includesMerge.asInstanceOf[(Seq[Entity], Seq[Any]) => Seq[Entity]]
    )
  }

  val defaultHasOneExtractors =
    new mutable.LinkedHashSet[HasOneExtractor[Entity]]()

  def extractHasOne[That](
    mapper: AssociationsFeature[That],
    fk: String,
    alias: Alias[That],
    merge: (Entity, Option[That]) => Entity,
    includesMerge: (Seq[Entity], Seq[That]) => Seq[Entity] =
      defaultIncludesMerge[Entity, That]
  ): HasOneExtractor[Entity] = {

    HasOneExtractor[Entity](
      mapper = mapper,
      fk = fk,
      alias = alias.asInstanceOf[Alias[Any]],
      merge = merge.asInstanceOf[(Entity, Option[Any]) => Entity],
      includesMerge =
        includesMerge.asInstanceOf[(Seq[Entity], Seq[Any]) => Seq[Entity]]
    )
  }

  // -----------------------------------------
  // One to Many Relation
  // -----------------------------------------

  val defaultOneToManyExtractors =
    new mutable.LinkedHashSet[HasManyExtractor[Entity]]()

  /**
   * One-to-Many relationship definition.
   *
   * {{{
   * object Member extends RelationshipFeature[Member] {
   *   oneToMany[Group](
   *     mapper = Group,
   *     merge = (m, c) => m.copy(company = c)
   *   )
   * }
   * }}}
   */
  def extractOneToMany[M1](
    mapper: AssociationsFeature[M1],
    fk: String,
    alias: Alias[M1],
    merge: (Entity, Seq[M1]) => Entity,
    includesMerge: (Seq[Entity], Seq[M1]) => Seq[Entity] =
      defaultIncludesMerge[Entity, M1]
  ): HasManyExtractor[Entity] = {

    if (defaultOneToManyExtractors.size > 5) {
      throw new IllegalStateException(
        "scalikejdbc ORM doesn't support more than 5 one-to-many tables."
      )
    }

    HasManyExtractor[Entity](
      mapper = mapper,
      fk = fk,
      alias = alias.asInstanceOf[Alias[Any]],
      merge = merge.asInstanceOf[(Entity, Seq[Any]) => Entity],
      includesMerge =
        includesMerge.asInstanceOf[(Seq[Entity], Seq[Any]) => Seq[Entity]]
    )
  }

  /**
   * Expects mapper's name + primary key name by default.
   *
   * @param mapper mapper
   * @tparam A entity type
   * @return fk name
   */
  protected def toDefaultForeignKeyName[A](
    mapper: AssociationsFeature[A]
  ): String = {
    val name: String = {
      JavaReflectionUtil.classSimpleName(mapper).replaceFirst("\\$$", "") +
        mapper.primaryKeyFieldName.head.toString.toUpperCase + mapper.primaryKeyFieldName.tail
    }
    name.head.toString.toLowerCase + name.tail
  }

  def selectQueryWithAssociations: SelectSQLBuilder[Entity] = {
    selectQueryWithAdditionalAssociations(
      defaultSelectQuery,
      belongsToAssociations,
      hasOneAssociations,
      hasManyAssociations
    )
  }

  def countQueryWithAssociations: SelectSQLBuilder[Entity] = {
    selectQueryWithAdditionalAssociations(
      simpleCountQuery,
      belongsToAssociations,
      hasOneAssociations,
      hasManyAssociations
    )
  }
}
