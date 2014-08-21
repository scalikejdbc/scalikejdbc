package scalikejdbc

import scala.reflect.macros.blackbox.Context

private[scalikejdbc] object MacroCompatible {

  def decls[C <: Context](c: C)(tpe: c.universe.Type) = tpe.decls

  def paramLists[C <: Context](c: C)(member: c.universe.MethodSymbol) = member.paramLists

}

