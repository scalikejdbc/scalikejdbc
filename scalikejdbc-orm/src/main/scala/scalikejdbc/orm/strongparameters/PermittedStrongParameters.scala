package scalikejdbc.orm.strongparameters

/**
 * Permitted strong parameters.
 *
 * @param params params
 */
class PermittedStrongParameters(val params: Map[String, (Any, ParamType)])
