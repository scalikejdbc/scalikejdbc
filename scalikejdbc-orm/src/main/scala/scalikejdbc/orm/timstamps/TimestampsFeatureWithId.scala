package scalikejdbc.orm.timstamps

import org.joda.time.DateTime

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.crud.CRUDFeatureWithId
import scalikejdbc.orm.strongparameters.PermittedStrongParameters

/**
  * ActiveRecord timestamps feature.
  *
  * @tparam Entity entity
  */
trait TimestampsFeatureWithId[Id, Entity]
  extends CRUDFeatureWithId[Id, Entity]
  with TimestampsFeatureBase[Entity] {

  override protected def namedValuesForCreation(
    strongParameters: PermittedStrongParameters
  ): Seq[(SQLSyntax, Any)] = {
    val additionalValues = timestampValues(strongParameters.params.contains)
    super.namedValuesForCreation(strongParameters) ++ additionalValues
  }

  override def createWithNamedValues(
    namedValues: (SQLSyntax, Any)*
  )(implicit s: DBSession = autoSession): Id = {
    val additionalValues =
      timestampValues(name => namedValues.exists(_._1 == column.field(name)))
    super.createWithNamedValues((namedValues ++ additionalValues)*)
  }

  override def updateBy(where: SQLSyntax): UpdateOperationBuilder = {
    val builder = super.updateBy(where)
    builder.addAttributeToBeUpdated(
      column.field(updatedAtFieldName) -> DateTime.now
    )
    builder
  }

}
