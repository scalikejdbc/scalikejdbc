package scalikejdbc

import scala.language.experimental.macros
import scalikejdbc.interpolation.SQLSyntax

trait SelectDynamicMacro[A] {

  /**
   * Returns [[scalikejdbc.interpolation.SQLSyntax]] value for the column which is referred by the field.
   */
  def selectDynamic(name: String): SQLSyntax =
    macro scalikejdbc.SQLInterpolationMacro.selectDynamic[A]
}
