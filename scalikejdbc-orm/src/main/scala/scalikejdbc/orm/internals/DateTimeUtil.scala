package scalikejdbc.orm.internals

import scala.language.implicitConversions
import org.joda.time.{ DateTime, LocalDate, LocalTime }
import scalikejdbc.orm.strongparameters.ParamType

import scala.collection.compat.*
import scala.util.Try

/**
 * DateTime utility.
 */
object DateTimeUtil {

  private val slashRegExp = "/".r
  private val baseRegExp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}".r

  private val timeZone1RegExp =
    "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}".r
  private val timeZone2RegExp =
    "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d+[+-]\\d{2}:\\d{2}".r

  private val dateTimeRegExp =
    "\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}".r

  private val timeZoneRegExp = "([+-]\\d{2}:\\d{2})".r

  private val whitespaceRegExp = "\\s+".r
  private val whitespaceSplitRegExp = "[-:\\s/]".r

  /**
   * The ISO8601 standard date format.
   */
  // val ISO_DATE_TIME_FORMAT = "%04d-%02d-%02dT%02d:%02d:%02d%s"
  val ISO_DATE_TIME_FORMAT = "%s-%s-%sT%s:%s:%s%s"

  /**
   * Returns current timezone value (e.g. +09:00).
   */
  def currentTimeZone: String = {
    val minutes = java.util.TimeZone.getDefault.getRawOffset / 1000 / 60
    (if (minutes >= 0) "+" else "-") + "%02d:%02d".format(
      (math.abs(minutes) / 60),
      (math.abs(minutes) % 60)
    )
  }

  /**
   * Returns "2014-01-02 03:04:05".
   */
  def toString(d: DateTime): String = d.toString("YYYY-MM-dd HH:mm:ss")

  def toString(d: LocalDate): String = d.toString("YYYY-MM-dd")

  def toString(d: LocalTime): String = d.toString("HH:mm:ss")

  /**
   * Returns "2014-01-02 03:04:05".
   */
  def nowString: String = toString(DateTime.now)

  private case class ZeroPaddingString(s: String) {
    def to04d: String = {
      try "%04d".format(s.toInt)
      catch {
        case e: NumberFormatException => s
      }
    }

    def to02d: String = {
      try "%02d".format(s.toInt)
      catch {
        case e: NumberFormatException => s
      }
    }
  }

  private implicit def fromStringToZeroPadding(s: String): ZeroPaddingString =
    ZeroPaddingString(s)

  /**
   * Converts string value to ISO8601 date format if possible.
   *
   * @param s         string value
   * @param paramType DateTime/LocalDate/LocalTime
   * @return ISO8601 data format string value
   */
  def toISODateTimeFormat(s: String, paramType: ParamType): String = {
    val str = slashRegExp.replaceAllIn(s, "-")
    if (baseRegExp.pattern.matcher(str).matches()) {
      val timeZone = timeZoneRegExp.findFirstIn(s).getOrElse(currentTimeZone)
      str + timeZone
    } else if (
      timeZone1RegExp.pattern.matcher(str).matches()
      || timeZone2RegExp.pattern.matcher(str).matches()
    ) {
      str
    } else if (dateTimeRegExp.pattern.matcher(str).matches()) {
      whitespaceRegExp.replaceFirstIn(str, "T")
    } else {
      whitespaceSplitRegExp.split(str).toList match {
        case year :: month :: day :: hour :: minute :: second :: zoneHour :: zoneMinute :: _ =>
          val timeZone =
            timeZoneRegExp.findFirstIn(str).getOrElse(currentTimeZone)
          ISO_DATE_TIME_FORMAT.format(
            year.to04d,
            month.to02d,
            day.to02d,
            hour.to02d,
            minute.to02d,
            second.to02d,
            timeZone
          )
        case year :: month :: day :: hour :: minute :: second :: _ =>
          ISO_DATE_TIME_FORMAT.format(
            year.to04d,
            month.to02d,
            day.to02d,
            hour.to02d,
            minute.to02d,
            second.to02d,
            currentTimeZone
          )
        case year :: month :: day :: hour :: minute :: _ =>
          ISO_DATE_TIME_FORMAT.format(
            year.to04d,
            month.to02d,
            day.to02d,
            hour.to02d,
            minute.to02d,
            "00",
            currentTimeZone
          )
        case year :: month :: day :: _ if paramType == ParamType.LocalDate =>
          ISO_DATE_TIME_FORMAT.format(
            year.to04d,
            month.to02d,
            day.to02d,
            "00",
            "00",
            "00",
            currentTimeZone
          )
        case hour :: minute :: second :: _
          if paramType == ParamType.LocalTime =>
          ISO_DATE_TIME_FORMAT.format(
            "1970",
            "01",
            "01",
            hour.to02d,
            minute.to02d,
            second.to02d,
            currentTimeZone
          )
        case hour :: minute :: _ if paramType == ParamType.LocalTime =>
          ISO_DATE_TIME_FORMAT.format(
            "1970",
            "01",
            "01",
            hour.to02d,
            minute.to02d,
            "00",
            currentTimeZone
          )
        case _ => str
      }
    }
  }

  def parseDateTime(s: String): DateTime =
    DateTime.parse(toISODateTimeFormat(s, ParamType.DateTime))

  def parseLocalDate(s: String): LocalDate =
    DateTime.parse(toISODateTimeFormat(s, ParamType.LocalDate)).toLocalDate

  def parseLocalTime(s: String): LocalTime =
    DateTime.parse(toISODateTimeFormat(s, ParamType.LocalTime)).toLocalTime

  def toDateString(
    params: Map[String, Any],
    year: String = "year",
    month: String = "month",
    day: String = "day"
  ): Option[String] = {

    try {
      (params.get(year).filterNot(_.toString.isEmpty) orElse
        params.get(month).filterNot(_.toString.isEmpty) orElse
        params.get(day).filterNot(_.toString.isEmpty)).map { _ =>
        "%04d-%02d-%02d".format(
          params.get(year).flatMap(_.toString.toIntOption).orNull,
          params.get(month).flatMap(_.toString.toIntOption).orNull,
          params.get(day).flatMap(_.toString.toIntOption).orNull
        )
      }
    } catch {
      case e: NumberFormatException => None
    }
  }

  def toUnsafeDateString(
    params: Map[String, Any],
    year: String = "year",
    month: String = "month",
    day: String = "day"
  ): Option[String] = {

    (params.get(year).filterNot(_.toString.isEmpty) orElse
      params.get(month).filterNot(_.toString.isEmpty) orElse
      params.get(day).filterNot(_.toString.isEmpty)).map { t =>
      "%s-%s-%s".format(
        params.get(year).map(_.toString.to04d).orNull,
        params.get(month).map(_.toString.to02d).orNull,
        params.get(day).map(_.toString.to02d).orNull
      )
    }
  }

  def toTimeString(
    params: Map[String, Any],
    hour: String = "hour",
    minute: String = "minute",
    second: String = "second"
  ): Option[String] = {

    try {
      (params.get(hour).filterNot(_.toString.isEmpty) orElse
        params.get(minute).filterNot(_.toString.isEmpty) orElse
        params.get(second).filterNot(_.toString.isEmpty)).map { _ =>
        "1970-01-01 %02d:%02d:%02d".format(
          params.get(hour).flatMap(_.toString.toIntOption).orNull,
          params.get(minute).flatMap(_.toString.toIntOption).orNull,
          params.get(second).flatMap(_.toString.toIntOption).orNull
        )
      }
    } catch {
      case e: NumberFormatException => None
    }
  }

  def toUnsafeTimeString(
    params: Map[String, Any],
    hour: String = "hour",
    minute: String = "minute",
    second: String = "second"
  ): Option[String] = {

    (params.get(hour).filterNot(_.toString.isEmpty) orElse
      params.get(minute).filterNot(_.toString.isEmpty) orElse
      params.get(second).filterNot(_.toString.isEmpty)).map { _ =>
      "1970-01-01 %s:%s:%s".format(
        params.get(hour).map(_.toString.to02d).orNull,
        params.get(minute).map(_.toString.to02d).orNull,
        params.get(second).map(_.toString.to02d).orNull
      )
    }
  }

  def toDateTimeString(
    params: Map[String, Any],
    year: String = "year",
    month: String = "month",
    day: String = "day",
    hour: String = "hour",
    minute: String = "minute",
    second: String = "second"
  ): Option[String] = {

    try {
      (params.get(year).filterNot(_.toString.isEmpty) orElse
        params.get(month).filterNot(_.toString.isEmpty) orElse
        params.get(day).filterNot(_.toString.isEmpty) orElse
        params.get(hour).filterNot(_.toString.isEmpty) orElse
        params.get(minute).filterNot(_.toString.isEmpty) orElse
        params.get(second).filterNot(_.toString.isEmpty)).map { _ =>
        "%04d-%02d-%02d %02d:%02d:%02d".format(
          params.get(year).flatMap(_.toString.toIntOption).orNull,
          params.get(month).flatMap(_.toString.toIntOption).orNull,
          params.get(day).flatMap(_.toString.toIntOption).orNull,
          params.get(hour).flatMap(_.toString.toIntOption).orNull,
          params.get(minute).flatMap(_.toString.toIntOption).orNull,
          params.get(second).flatMap(_.toString.toIntOption).orNull
        )
      }
    } catch {
      case e: NumberFormatException => None
    }
  }

  def toUnsafeDateTimeString(
    params: Map[String, Any],
    year: String = "year",
    month: String = "month",
    day: String = "day",
    hour: String = "hour",
    minute: String = "minute",
    second: String = "second"
  ): Option[String] = {

    (params.get(year).filterNot(_.toString.isEmpty) orElse
      params.get(month).filterNot(_.toString.isEmpty) orElse
      params.get(day).filterNot(_.toString.isEmpty) orElse
      params.get(hour).filterNot(_.toString.isEmpty) orElse
      params.get(minute).filterNot(_.toString.isEmpty) orElse
      params.get(second).filterNot(_.toString.isEmpty)).map { _ =>
      "%s-%s-%s %s:%s:%s".format(
        params.get(year).map(_.toString.to04d).orNull,
        params.get(month).map(_.toString.to02d).orNull,
        params.get(day).map(_.toString.to02d).orNull,
        params.get(hour).map(_.toString.to02d).orNull,
        params.get(minute).map(_.toString.to02d).orNull,
        params.get(second).map(_.toString.to02d).orNull
      )
    }
  }

  def toUnsafeDateTimeStringFromDateAndTime(
    params: Map[String, Any],
    date: String = "date",
    time: String = "time"
  ): Option[String] = {

    (params.get(date).filterNot(_.toString.isEmpty) orElse
      params.get(time).filterNot(_.toString.isEmpty)).map { _ =>
      "%s %s".format(
        params.get(date).map(_.toString).orNull,
        params.get(time).map(_.toString).orNull
      )
    }
  }

  def isLocalDateFormat(str: String): Boolean = Try(
    parseLocalDate(str)
  ).isSuccess

  def isDateTimeFormat(str: String): Boolean = Try(parseDateTime(str)).isSuccess

}
