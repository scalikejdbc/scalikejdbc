package scalikejdbc

private[scalikejdbc] object MacroCompatible {

  type Context = scala.reflect.macros.Context

  def decls(c: Context)(tpe: c.universe.Type) = tpe.declarations

  def paramLists(c: Context)(member: c.universe.MethodSymbol) = member.paramss

}

