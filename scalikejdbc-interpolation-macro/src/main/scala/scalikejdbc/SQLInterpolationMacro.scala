package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros._

object SQLInterpolationMacro {

// TODO still not working..
/*
scala> import scala.language.experimental.macros
import scala.language.experimental.macros

scala> import scalikejdbc.SQLInterpolationMacro._
import scalikejdbc.SQLInterpolationMacro._

scala> def v(name: String, expectedNames: Seq[String]) = macro validateFieldImpl
v: (name: String, expectedNames: Seq[String])String

scala> val (name, expectedNames) = ("a", Seq("a", "b"))
name: String = a
expectedNames: Seq[String] = List(a, b)

scala> v(name, expectedNames)
res0: String = a

scala> val (name, expectedNames) = ("a", Seq("aa", "b"))
name: String = a
expectedNames: Seq[String] = List(aa, b)

scala> v(name, expectedNames)
<console>:15: error: Invalid field name 'a' not found in (aa,b)
              v(name, expectedNames)
               ^

scala> v("a", Seq("a", "b"))
error: exception during macro expansion:
scala.tools.reflect.ToolBoxError: reflective toolbox has failed: cannot operate on trees that are already typed
	at scala.tools.reflect.ToolBoxFactory$ToolBoxImpl$ToolBoxGlobal.verify(ToolBoxFactory.scala:74)
	at scala.tools.reflect.ToolBoxFactory$ToolBoxImpl$ToolBoxGlobal.compile(ToolBoxFactory.scala:200)
	at scala.tools.reflect.ToolBoxFactory$ToolBoxImpl.compile(ToolBoxFactory.scala:415)
	at scala.tools.reflect.ToolBoxFactory$ToolBoxImpl.eval(ToolBoxFactory.scala:418)
	at scala.reflect.macros.runtime.Evals$class.eval(Evals.scala:16)
	at scala.reflect.macros.runtime.Context.eval(Context.scala:6)
	at scalikejdbc.SQLInterpolationMacro$.validateFieldImpl(SQLInterpolationMacro.scala:11)
*/

  def validateFieldImpl(c: Context)(name: c.Expr[String], expectedNames: c.Expr[Seq[String]]): c.Expr[String] = {
    import c.universe._
    val _name = c.eval(c.Expr[String](c.resetAllAttrs(name.tree.duplicate)))
    val _expectedNames = c.eval(c.Expr[Seq[String]](c.resetAllAttrs(expectedNames.tree.duplicate)))
    if (!_expectedNames.contains(_name)) {
      c.error(c.enclosingPosition, s"Invalid field name '${_name}' not found in (${_expectedNames.mkString(",")})")
    }
    name
  }

}
