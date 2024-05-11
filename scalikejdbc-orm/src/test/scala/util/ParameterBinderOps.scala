package util

import scalikejdbc.{ ParameterBinderWithValue, _ }

import scala.annotation.tailrec

trait ParameterBinderOps {

  @tailrec
  final def extractValueFromParameterBinder(
    kv: (SQLSyntax, Any)
  ): (SQLSyntax, Any) = {
    kv match {
      case (k, v: ParameterBinderWithValue) =>
        extractValueFromParameterBinder((k, v.value))
      case (k, v) => (k, v)
    }
  }

  @tailrec
  final def extractValueFromParameterBinder(v: Any): Any = {
    v match {
      case v: ParameterBinderWithValue =>
        extractValueFromParameterBinder(v.value)
      case _ => v
    }
  }

}

object ParameterBinderOps extends ParameterBinderOps
