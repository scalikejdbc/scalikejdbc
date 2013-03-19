package scalikejdbc.interpolation

import scala.language.experimental.macros
import scala.reflect.macros._

object SQLSyntaxFieldMacro {

  def validate(name: String, expectedNames: Seq[String]): String = macro validateImpl

  def validateImpl(c: Context)(name: c.Expr[String], expectedNames: c.Expr[Seq[String]]): c.Expr[String] = {
    import c.universe._
    // TODO eval doesn't work ...
    val _name: String = c.eval(name)
    val _expectedNames: Seq[String] = c.eval(expectedNames)
    if (!_expectedNames.contains(_name)) {
      c.error(c.enclosingPosition, s"Invalid field name ${_name} (expected: ${_expectedNames.mkString(",")}")
    }
    reify(_name)
  }

}
