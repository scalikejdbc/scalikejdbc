package scalikejdbc.mapper

object DateTimeClass {
  case object JodaDateTime extends DateTimeClass("org.joda.time.DateTime")
  case object ZonedDateTime extends DateTimeClass("java.time.ZonedDateTime")
  case object OffsetDateTime extends DateTimeClass("java.time.OffsetDateTime")
  case object LocalDateTime extends DateTimeClass("java.time.LocalDateTime")
  case object Instant extends DateTimeClass("java.time.Instant")

  private[scalikejdbc] val all =
    Set(JodaDateTime, ZonedDateTime, OffsetDateTime, LocalDateTime, Instant)

  private[scalikejdbc] val map: Map[String, DateTimeClass] =
    all.map(clazz => clazz.name -> clazz).toMap
}

sealed abstract class DateTimeClass(private[scalikejdbc] val name: String)
  extends Product
  with Serializable {
  private[scalikejdbc] val simpleName = name.split('.').last
}
