/*
 * Copyright 2013 Manabu Nakamura
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

import java.sql.ResultSet
import java.util.Calendar
import org.joda.time._
import collection.JavaConverters._

/**
 * Type binder for java.sql.ResultSet.
 */
trait TypeBinder[+A] {
  def apply(rs: ResultSet, columnIndex: Int): A
  def apply(rs: ResultSet, columnLabel: String): A
  def map[B](f: A => B): TypeBinder[B] = new TypeBinder[B] {
    def apply(rs: ResultSet, columnIndex: Int): B = f(TypeBinder.this.apply(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): B = f(TypeBinder.this.apply(rs, columnLabel))
  }
}

/**
 * Type binder for java.sql.ResultSet.
 */
object TypeBinder extends LowPriorityTypeBinderImplicits {

  def apply[A](index: (ResultSet, Int) => A)(label: (ResultSet, String) => A): TypeBinder[A] = new TypeBinder[A] {
    def apply(rs: ResultSet, columnIndex: Int): A = index(rs, columnIndex)
    def apply(rs: ResultSet, columnLabel: String): A = label(rs, columnLabel)
  }

  private[scalikejdbc] val any: TypeBinder[Any] = TypeBinder(_ getObject _)(_ getObject _)
  implicit val array: TypeBinder[java.sql.Array] = TypeBinder(_ getArray _)(_ getArray _)
  implicit val bigDecimal: TypeBinder[java.math.BigDecimal] = TypeBinder(_ getBigDecimal _)(_ getBigDecimal _)
  implicit val binaryStream: TypeBinder[java.io.InputStream] = TypeBinder(_ getBinaryStream _)(_ getBinaryStream _)
  implicit val blob: TypeBinder[java.sql.Blob] = TypeBinder(_ getBlob _)(_ getBlob _)
  implicit val nullableBoolean: TypeBinder[java.lang.Boolean] = any.map {
    case b if b == null => b.asInstanceOf[java.lang.Boolean]
    case b: java.lang.Boolean => b
    case b: Boolean => b.asInstanceOf[java.lang.Boolean]
    case s: String => {
      try s.toInt != 0
      catch { case e: NumberFormatException => !s.isEmpty }
    }.asInstanceOf[java.lang.Boolean]
    case v => (v != 0).asInstanceOf[java.lang.Boolean]
  }
  implicit val boolean: TypeBinder[Boolean] = nullableBoolean.map(throwExceptionIfNull(_.asInstanceOf[Boolean]))
  implicit val optionBoolean: TypeBinder[Option[Boolean]] = nullableBoolean.map(v => Option(v).map(_.asInstanceOf[Boolean]))
  implicit val nullableByte: TypeBinder[java.lang.Byte] = any.map(v => if (v == null) null else java.lang.Byte.valueOf(v.toString))
  implicit val byte: TypeBinder[Byte] = nullableByte.map(throwExceptionIfNull(_.asInstanceOf[Byte]))
  implicit val optionByte: TypeBinder[Option[Byte]] = nullableByte.map(v => Option(v).map(_.asInstanceOf[Byte]))
  implicit val bytes: TypeBinder[Array[Byte]] = TypeBinder(_ getBytes _)(_ getBytes _)
  implicit val characterStream: TypeBinder[java.io.Reader] = TypeBinder(_ getCharacterStream _)(_ getCharacterStream _)
  implicit val clob: TypeBinder[java.sql.Clob] = TypeBinder(_ getClob _)(_ getClob _)
  implicit val date: TypeBinder[java.sql.Date] = TypeBinder(_ getDate _)(_ getDate _)
  implicit val nullableDouble: TypeBinder[java.lang.Double] = any.map(v => if (v == null) null else java.lang.Double.valueOf(v.toString))
  implicit val double: TypeBinder[Double] = nullableDouble.map(throwExceptionIfNull(_.asInstanceOf[Double]))
  implicit val optionDouble: TypeBinder[Option[Double]] = nullableDouble.map(v => Option(v).map(_.asInstanceOf[Double]))
  implicit val nullableFloat: TypeBinder[java.lang.Float] = any.map(v => if (v == null) null else java.lang.Float.valueOf(v.toString))
  implicit val float: TypeBinder[Float] = nullableFloat.map(throwExceptionIfNull(_.asInstanceOf[Float]))
  implicit val optionFloat: TypeBinder[Option[Float]] = nullableFloat.map(v => Option(v).map(_.asInstanceOf[Float]))
  implicit val nullableInt: TypeBinder[java.lang.Integer] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Integer]
    case v: Float => v.toInt.asInstanceOf[java.lang.Integer]
    case v: Double => v.toInt.asInstanceOf[java.lang.Integer]
    case v => java.lang.Integer.valueOf(v.toString)
  }
  implicit val int: TypeBinder[Int] = nullableInt.map(throwExceptionIfNull(_.asInstanceOf[Int]))
  implicit val optionInt: TypeBinder[Option[Int]] = nullableInt.map(v => Option(v).map(_.asInstanceOf[Int]))
  implicit val nullableLong: TypeBinder[java.lang.Long] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Long]
    case v: Float => v.toLong.asInstanceOf[java.lang.Long]
    case v: Double => v.toLong.asInstanceOf[java.lang.Long]
    case v => java.lang.Long.valueOf(v.toString)
  }
  implicit val long: TypeBinder[Long] = nullableLong.map(throwExceptionIfNull(_.asInstanceOf[Long]))
  implicit val optionLong: TypeBinder[Option[Long]] = nullableLong.map(v => Option(v).map(_.asInstanceOf[Long]))
  implicit val nClob: TypeBinder[java.sql.NClob] = TypeBinder(_ getNClob _)(_ getNClob _)
  implicit val ref: TypeBinder[java.sql.Ref] = TypeBinder(_ getRef _)(_ getRef _)
  implicit val rowId: TypeBinder[java.sql.RowId] = TypeBinder(_ getRowId _)(_ getRowId _)
  implicit val nullableShort: TypeBinder[java.lang.Short] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Short]
    case v: Float => v.toShort.asInstanceOf[java.lang.Short]
    case v: Double => v.toShort.asInstanceOf[java.lang.Short]
    case v => java.lang.Short.valueOf(v.toString)
  }
  implicit val short: TypeBinder[Short] = nullableShort.map(throwExceptionIfNull(_.asInstanceOf[Short]))
  implicit val optionShort: TypeBinder[Option[Short]] = nullableShort.map(v => Option(v).map(_.asInstanceOf[Short]))
  implicit val sqlXml: TypeBinder[java.sql.SQLXML] = TypeBinder(_ getSQLXML _)(_ getSQLXML _)
  implicit val string: TypeBinder[String] = TypeBinder(_ getString _)(_ getString _)
  implicit val time: TypeBinder[java.sql.Time] = TypeBinder(_ getTime _)(_ getTime _)
  implicit val timestamp: TypeBinder[java.sql.Timestamp] = TypeBinder(_ getTimestamp _)(_ getTimestamp _)
  implicit val url: TypeBinder[java.net.URL] = TypeBinder(_ getURL _)(_ getURL _)
  implicit val dateTime: TypeBinder[DateTime] = option[java.sql.Timestamp].map(_.map(_.toDateTime).orNull[DateTime])
  implicit val localDate: TypeBinder[LocalDate] = option[java.sql.Date].map(_.map(_.toLocalDate).orNull[LocalDate])
  implicit val localTime: TypeBinder[LocalTime] = option[java.sql.Time].map(_.map(_.toLocalTime).orNull[LocalTime])

  private[scalikejdbc] val asciiStream: TypeBinder[java.io.InputStream] = TypeBinder(_ getAsciiStream _)(_ getAsciiStream _)
  private[scalikejdbc] val nCharacterStream: TypeBinder[java.io.Reader] = TypeBinder(_ getNCharacterStream _)(_ getNCharacterStream _)
  private[scalikejdbc] val nString: TypeBinder[String] = TypeBinder(_ getNString _)(_ getNString _)

  private def throwExceptionIfNull[A <: AnyVal](f: Any => A)(a: Any): A =
    if (a == null) throw new UnexpectedNullValueException else f(a)

}

trait LowPriorityTypeBinderImplicits {

  implicit def option[A](implicit ev: TypeBinder[A]): TypeBinder[Option[A]] = new TypeBinder[Option[A]] {
    def apply(rs: ResultSet, columnIndex: Int): Option[A] = wrap(ev(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): Option[A] = wrap(ev(rs, columnLabel))
    private def wrap[A](a: => A): Option[A] =
      try Option(a) catch { case _: NullPointerException | _: UnexpectedNullValueException => None }
  }

}
