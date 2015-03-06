/*
 * Copyright 2011 - 2015 scalikejdbc.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc.jsr310

import java.util.{ GregorianCalendar, Calendar }

import scala.language.implicitConversions
import java.time._
import scalikejdbc.{ TypeBinder, WrappedResultSet }

object Implicits extends Implicits

trait Implicits {

  import TypeBinder._

  implicit def fromWrappedResultSetToJSR310WrappedResultSet(rs: WrappedResultSet): JSR310WrappedResultSet =
    new JSR310WrappedResultSet(rs)

  implicit val zonedDateTime: TypeBinder[ZonedDateTime] = option[java.sql.Timestamp].map(_.map(v => ZonedDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())).orNull[ZonedDateTime])
  implicit val offsetDateTime: TypeBinder[OffsetDateTime] = option[java.sql.Timestamp].map(_.map(v => OffsetDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())).orNull[OffsetDateTime])
  implicit val localDate: TypeBinder[LocalDate] = option[java.sql.Date].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDate).orNull[LocalDate])
  implicit val localTime: TypeBinder[LocalTime] = option[java.sql.Time].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalTime).orNull[LocalTime])
  implicit val localDateTime: TypeBinder[LocalDateTime] = option[java.sql.Timestamp].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDateTime).orNull[LocalDateTime])

  implicit val zonedDateTimeOpt: TypeBinder[Option[ZonedDateTime]] = option[java.sql.Timestamp].map(_.map(v => ZonedDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())))
  implicit val offsetDateTimeOpt: TypeBinder[Option[OffsetDateTime]] = option[java.sql.Timestamp].map(_.map(v => OffsetDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())))
  implicit val localDateOpt: TypeBinder[Option[LocalDate]] = option[java.sql.Date].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDate))
  implicit val localTimeOpt: TypeBinder[Option[LocalTime]] = option[java.sql.Time].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalTime))
  implicit val localDateTimeOpt: TypeBinder[Option[LocalDateTime]] = option[java.sql.Timestamp].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDateTime))

}
