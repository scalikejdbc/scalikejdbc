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

import scalikejdbc.WrappedResultSet
import java.time._

/**
 * Rich WrappedResultSet for JSR 310 support.
 */
class JSR310WrappedResultSet(underlying: WrappedResultSet) extends Implicits {

  def dateTime(columnIndex: Int): ZonedDateTime = zonedDateTime(columnIndex)
  def dateTime(columnLabel: String): ZonedDateTime = zonedDateTime(columnLabel)

  def zonedDateTime(columnIndex: Int): ZonedDateTime = underlying.get[ZonedDateTime](columnIndex)
  def zonedDateTime(columnLabel: String): ZonedDateTime = underlying.get[ZonedDateTime](columnLabel)

  def offsetDateTime(columnIndex: Int): OffsetDateTime = underlying.get[OffsetDateTime](columnIndex)
  def offsetDateTime(columnLabel: String): OffsetDateTime = underlying.get[OffsetDateTime](columnLabel)

  def localDate(columnIndex: Int): LocalDate = underlying.get[LocalDate](columnIndex)
  def localDate(columnLabel: String): LocalDate = underlying.get[LocalDate](columnLabel)

  def localTime(columnIndex: Int): LocalTime = underlying.get[LocalTime](columnIndex)
  def localTime(columnLabel: String): LocalTime = underlying.get[LocalTime](columnLabel)

  def localDateTime(columnIndex: Int): LocalDateTime = underlying.get[LocalDateTime](columnIndex)
  def localDateTime(columnLabel: String): LocalDateTime = underlying.get[LocalDateTime](columnLabel)

  def dateTimeOpt(columnIndex: Int): Option[ZonedDateTime] = zonedDateTimeOpt(columnIndex)
  def dateTimeOpt(columnLabel: String): Option[ZonedDateTime] = zonedDateTimeOpt(columnLabel)

  def zonedDateTimeOpt(columnIndex: Int): Option[ZonedDateTime] = underlying.get[Option[ZonedDateTime]](columnIndex)
  def zonedDateTimeOpt(columnLabel: String): Option[ZonedDateTime] = underlying.get[Option[ZonedDateTime]](columnLabel)

  def offsetDateTimeOpt(columnIndex: Int): Option[OffsetDateTime] = underlying.get[Option[OffsetDateTime]](columnIndex)
  def offsetDateTimeOpt(columnLabel: String): Option[OffsetDateTime] = underlying.get[Option[OffsetDateTime]](columnLabel)

  def localDateOpt(columnIndex: Int): Option[LocalDate] = underlying.get[Option[LocalDate]](columnIndex)
  def localDateOpt(columnLabel: String): Option[LocalDate] = underlying.get[Option[LocalDate]](columnLabel)

  def localTimeOpt(columnIndex: Int): Option[LocalTime] = underlying.get[Option[LocalTime]](columnIndex)
  def localTimeOpt(columnLabel: String): Option[LocalTime] = underlying.get[Option[LocalTime]](columnLabel)

  def localDateTimeOpt(columnIndex: Int): Option[LocalDateTime] = underlying.get[Option[LocalDateTime]](columnIndex)
  def localDateTimeOpt(columnLabel: String): Option[LocalDateTime] = underlying.get[Option[LocalDateTime]](columnLabel)

}
