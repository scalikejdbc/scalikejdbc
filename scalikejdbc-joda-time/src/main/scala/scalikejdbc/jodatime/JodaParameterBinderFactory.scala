package scalikejdbc
package jodatime

object JodaParameterBinderFactory {

  implicit val jodaDateTimeParameterBinderFactory
    : ParameterBinderFactory[org.joda.time.DateTime] =
    JodaBinders.jodaDateTime
  implicit val jodaLocalDateTimeParameterBinderFactory
    : ParameterBinderFactory[org.joda.time.LocalDateTime] =
    JodaBinders.jodaLocalDateTime
  implicit val jodaLocalDateParameterBinderFactory
    : ParameterBinderFactory[org.joda.time.LocalDate] =
    JodaBinders.jodaLocalDate
  implicit val jodaLocalTimeParameterBinderFactory
    : ParameterBinderFactory[org.joda.time.LocalTime] =
    JodaBinders.jodaLocalTime

}
