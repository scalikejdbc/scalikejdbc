package scalikejdbc

import scala.runtime.BoxedUnit
import scala.language.implicitConversions
import scala.language.reflectiveCalls

object SQLInterpolation {
  @inline implicit def interpolation(s: StringContext) = new SQLInterpolation(s)
}

class SQLInterpolation(val s: StringContext) extends AnyVal {

  def sql[P](param: P) = {
    try {
      val tuple = param.asInstanceOf[{def productIterator: Iterator[Any]}]
      SQL(s.parts.mkString("?")).bind(tuple.productIterator.toList: _*)
    } catch { case _: Throwable =>
        param match {
          case _: BoxedUnit => SQL(s.parts.mkString("?"))
          case singleParam => SQL(s.parts.mkString("?")).bind(singleParam)
        }
    }
  }

}
