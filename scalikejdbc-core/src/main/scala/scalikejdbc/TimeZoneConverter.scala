package scalikejdbc

import java.sql.{ Time, Date, Timestamp }
import java.util.{ Calendar, TimeZone }

import scala.collection.concurrent.TrieMap

/**
 * TimeZone converter for time related types.
 */
class TimeZoneConverter(fromTimeZone: TimeZone, toTimeZone: TimeZone) {
  def convert(date: Date): Date =
    if (date != null) new Date(convertMillis(date)) else null

  def convert(time: Time): Time =
    if (time != null) new Time(convertMillis(time)) else null

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
      converterCache.getOrElse(toTimeZone, {
        val converter = new TimeZoneConverter(fromTimeZone, toTimeZone)
        converterCache(toTimeZone) = converter
        converter
      })
  }

  private[this] val builderCache: TrieMap[TimeZone, TimeZoneConverterBuilder] = TrieMap.empty

  def from(timeZone: TimeZone): TimeZoneConverterBuilder =
    builderCache.getOrElse(timeZone, {
      val builder = new TimeZoneConverterBuilder(timeZone)
      builderCache(timeZone) = builder
      builder
    })
}
