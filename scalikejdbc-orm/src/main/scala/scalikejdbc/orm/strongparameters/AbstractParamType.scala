package scalikejdbc.orm.strongparameters

/**
 * Basic template for ParamType implementation.
 */
abstract class AbstractParamType(matcher: PartialFunction[Any, Any])
  extends ParamType {

  override def unapply(value: Any): Option[Any] = {
    if (value == null) None else PartialFunction.condOpt(value)(matcher)
  }
}
