package scalikejdbc.mapper

object DateTimeClass {
  case object JodaDateTime extends DateTimeClass("org.joda.time.DateTime")
  case object ZonedDateTime extends DateTimeClass("java.time.ZonedDateTime")
  case object OffsetDateTime extends DateTimeClass("java.time.OffsetDateTime")

  private[scalikejdbc] val all = Set(
    JodaDateTime, ZonedDateTime, OffsetDateTime
  )

  private[scalikejdbc] val map: Map[String, DateTimeClass] =
    all.map(clazz => clazz.name -> clazz).toMap
}

sealed abstract class DateTimeClass(private[scalikejdbc] val name: String) extends Product with Serializable {
  private[scalikejdbc] val simpleName = name.split('.').last
}
