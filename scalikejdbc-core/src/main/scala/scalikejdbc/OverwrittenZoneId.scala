package scalikejdbc

import java.time.ZoneId

/** A hold of a specific ZoneId instance to be passed as an implicit parameter for TypeBinder. */
case class OverwrittenZoneId(value: ZoneId) extends AnyVal
