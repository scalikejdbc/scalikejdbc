package scalikejdbc

import scala.reflect.macros.blackbox.Context

private[scalikejdbc] object MacroCompatible {

  def decls(c: Context)(tpe: c.universe.Type) = tpe.declarations

  def paramLists(c: Context)(member: c.universe.MethodSymbol) = member.paramss

}

