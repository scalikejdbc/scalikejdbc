package scalikejdbc

import scalikejdbc.interpolation.SQLSyntax

trait SelectDynamicMacro[A] {
  // TODO
  def selectDynamic(name: String): SQLSyntax =
    ???
}
