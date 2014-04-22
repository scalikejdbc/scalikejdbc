/*
 * Copyright 2014 scalikejdbc.org
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
package scalikejdbc

import scala.language.implicitConversions

import java.sql.{ Timestamp => sqlTimestamp, Time => sqlTime, Date => sqlDate }
import java.util.{ Date => utilDate }
import org.joda.time.LocalTime

/**
 * Implicit conversions for date time values.
 */
trait UnixTimeInMillisConverterImplicits {

  implicit def convertJavaUtilDateToConverter(t: utilDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlDateToConverter(t: sqlDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlTimeToConverter(t: sqlTime): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlTimestampToConverter(t: sqlTimestamp): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertLocalTimeToConverter(t: LocalTime): LocalTimeConverter = new LocalTimeConverter(t)

}
