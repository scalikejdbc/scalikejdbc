package scalikejdbc

import java.util.TimeZone

/**
 * Settings for timezone conversion
 */
case class TimeZoneSettings(
  conversionEnabled: Boolean = false,
  serverTimeZone: TimeZone = TimeZone.getDefault
)
