package scalikejdbc

import java.sql.Timestamp
import java.util.{ Calendar, TimeZone }

import scala.collection.concurrent.TrieMap

/**
 * TimeZone converter for SQL Timestamp
 */
class TimeZoneConverter(fromTimeZone: TimeZone, toTimeZone: TimeZone) {

  def convert(timestamp: Timestamp): Timestamp =
    if (timestamp != null) new Timestamp(convertMillis(timestamp)) else null

  private[this] def convertMillis(utilDate: java.util.Date): Long = {
    val fromCal = Calendar.getInstance(fromTimeZone)
    fromCal.setTime(utilDate)
    val fromOffset = fromCal.get(Calendar.ZONE_OFFSET) + fromCal.get(Calendar.DST_OFFSET)

    val toCal = Calendar.getInstance(toTimeZone)
    toCal.setTime(utilDate)
    val toOffset = toCal.get(Calendar.ZONE_OFFSET) + toCal.get(Calendar.DST_OFFSET)

    val delta = toOffset - fromOffset

    utilDate.getTime + delta
  }
}

object TimeZoneConverter {
  class TimeZoneConverterBuilder(fromTimeZone: TimeZone) {
    private[this] val converterCache: TrieMap[TimeZone, TimeZoneConverter] = TrieMap.empty

    def to(toTimeZone: TimeZone): TimeZoneConverter =
      converterCache.getOrElseUpdate(toTimeZone, new TimeZoneConverter(fromTimeZone, toTimeZone))
  }

  private[this] val builderCache: TrieMap[TimeZone, TimeZoneConverterBuilder] = TrieMap.empty

  def from(timeZone: TimeZone): TimeZoneConverterBuilder =
    builderCache.getOrElseUpdate(timeZone, new TimeZoneConverterBuilder(timeZone))
}
