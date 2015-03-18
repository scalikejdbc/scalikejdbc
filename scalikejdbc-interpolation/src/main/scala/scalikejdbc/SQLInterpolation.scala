package scalikejdbc

@deprecated("'import SQLInterpolation._' is no longer required. Just 'import scalikejdbc._' works fine.", since = "2.0.0")
object SQLInterpolation {
}

/**
 * SQLInterpolation full imports.
 */
trait SQLInterpolation
  extends SQLInterpolationFeature
  with SQLSyntaxSupportFeature
  with QueryDSLFeature

