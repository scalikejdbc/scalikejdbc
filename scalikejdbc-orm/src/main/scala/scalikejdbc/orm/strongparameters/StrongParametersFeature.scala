package scalikejdbc.orm.strongparameters

import scala.annotation.tailrec

/**
 * Strong parameters support.
 */
trait StrongParametersFeature {

  /**
   * Returns typed value from a strong parameter.
   *
   * @param fieldName field name
   * @param value     actual value
   * @param paramType param type definition
   * @return typed value if exists
   */
  protected def getTypedValueFromStrongParameter(
    fieldName: String,
    value: Any,
    paramType: ParamType
  ): Option[Any] = {
    val ParamTypeExtractor = paramType
    Option(value).map {
      case Some(v) =>
        recFlattenOption(
          getTypedValueFromStrongParameter(fieldName, v, paramType)
        )
      case None                  => null
      case ParamTypeExtractor(v) => v
      case v: String if v == ""  => null
      case v: String             => v
      case v =>
        throw new IllegalArgumentException(
          s"Cannot convert '${v}' to ${paramType} value."
        )
    }
  }

  /**
   * Returns a recursively flattened version of the passed in argument if it is an Option
   *
   * @param maybeOption an Any that you suspect may be an Option
   * @return recursively flattened version of the passed in argument
   */
  @tailrec
  private def recFlattenOption(maybeOption: Any): Any = maybeOption match {
    case Some(x) => recFlattenOption(x)
    case _       => maybeOption // is not something that needs flattening
  }

}
