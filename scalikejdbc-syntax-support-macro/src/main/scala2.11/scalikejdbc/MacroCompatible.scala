package scalikejdbc

private[scalikejdbc] object MacroCompatible {

  type Context = scala.reflect.macros.blackbox.Context

  def decls[C <: Context](c: C)(tpe: c.universe.Type) = tpe.decls

  def paramLists[C <: Context](c: C)(member: c.universe.MethodSymbol) = member.paramLists

}

