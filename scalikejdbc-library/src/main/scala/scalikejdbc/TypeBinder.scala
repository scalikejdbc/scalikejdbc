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

  def apply[A](index: ResultSet => Int => A)(label: ResultSet => String => A): TypeBinder[A] = new TypeBinder[A] {
    def apply(rs: ResultSet, columnIndex: Int): A = index(rs)(columnIndex)
    def apply(rs: ResultSet, columnLabel: String): A = label(rs)(columnLabel)
  }

  private[scalikejdbc] val any: TypeBinder[Any] = TypeBinder(_.getObject)(_.getObject)
  implicit val array: TypeBinder[java.sql.Array] = TypeBinder(_.getArray)(_.getArray)
  implicit val bigDecimal: TypeBinder[java.math.BigDecimal] = TypeBinder(_.getBigDecimal)(_.getBigDecimal)
  implicit val binaryStream: TypeBinder[java.io.InputStream] = TypeBinder(_.getBinaryStream)(_.getBinaryStream)
  implicit val blob: TypeBinder[java.sql.Blob] = TypeBinder(_.getBlob)(_.getBlob)
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
  implicit val boolean: TypeBinder[Boolean] = nullableBoolean.map(_.asInstanceOf[Boolean])
  implicit val optionBoolean: TypeBinder[Option[Boolean]] = nullableBoolean.map(v => Option(v).map(_.asInstanceOf[Boolean]))
  implicit val nullableByte: TypeBinder[java.lang.Byte] = any.map(v => if (v == null) null else java.lang.Byte.valueOf(v.toString))
  implicit val byte: TypeBinder[Byte] = nullableByte.map(_.asInstanceOf[Byte])
  implicit val optionByte: TypeBinder[Option[Byte]] = nullableByte.map(v => Option(v).map(_.asInstanceOf[Byte]))
  implicit val bytes: TypeBinder[Array[Byte]] = TypeBinder(_.getBytes)(_.getBytes)
  implicit val characterStream: TypeBinder[java.io.Reader] = TypeBinder(_.getCharacterStream)(_.getCharacterStream)
  implicit val clob: TypeBinder[java.sql.Clob] = TypeBinder(_.getClob)(_.getClob)
  implicit val date: TypeBinder[java.sql.Date] = TypeBinder(_.getDate)(_.getDate)
  implicit val nullableDouble: TypeBinder[java.lang.Double] = any.map(v => if (v == null) null else java.lang.Double.valueOf(v.toString))
  implicit val double: TypeBinder[Double] = nullableDouble.map(_.asInstanceOf[Double])
  implicit val optionDouble: TypeBinder[Option[Double]] = nullableDouble.map(v => Option(v).map(_.asInstanceOf[Double]))
  implicit val nullableFloat: TypeBinder[java.lang.Float] = any.map(v => if (v == null) null else java.lang.Float.valueOf(v.toString))
  implicit val float: TypeBinder[Float] = nullableFloat.map(_.asInstanceOf[Float])
  implicit val optionFloat: TypeBinder[Option[Float]] = nullableFloat.map(v => Option(v).map(_.asInstanceOf[Float]))
  implicit val nullableInt: TypeBinder[java.lang.Integer] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Integer]
    case v: Float => v.toInt.asInstanceOf[java.lang.Integer]
    case v: Double => v.toInt.asInstanceOf[java.lang.Integer]
    case v => java.lang.Integer.valueOf(v.toString)
  }
  implicit val int: TypeBinder[Int] = nullableInt.map(_.asInstanceOf[Int])
  implicit val optionInt: TypeBinder[Option[Int]] = nullableInt.map(v => Option(v).map(_.asInstanceOf[Int]))
  implicit val nullableLong: TypeBinder[java.lang.Long] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Long]
    case v: Float => v.toLong.asInstanceOf[java.lang.Long]
    case v: Double => v.toLong.asInstanceOf[java.lang.Long]
    case v => java.lang.Long.valueOf(v.toString)
  }
  implicit val long: TypeBinder[Long] = nullableLong.map(_.asInstanceOf[Long])
  implicit val optionLong: TypeBinder[Option[Long]] = nullableLong.map(v => Option(v).map(_.asInstanceOf[Long]))
  implicit val nClob: TypeBinder[java.sql.NClob] = TypeBinder(_.getNClob)(_.getNClob)
  implicit val ref: TypeBinder[java.sql.Ref] = TypeBinder(_.getRef)(_.getRef)
  implicit val rowId: TypeBinder[java.sql.RowId] = TypeBinder(_.getRowId)(_.getRowId)
  implicit val nullableShort: TypeBinder[java.lang.Short] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Short]
    case v: Float => v.toShort.asInstanceOf[java.lang.Short]
    case v: Double => v.toShort.asInstanceOf[java.lang.Short]
    case v => java.lang.Short.valueOf(v.toString)
  }
  implicit val short: TypeBinder[Short] = nullableShort.map(_.asInstanceOf[Short])
  implicit val optionShort: TypeBinder[Option[Short]] = nullableShort.map(v => Option(v).map(_.asInstanceOf[Short]))
  implicit val sqlXml: TypeBinder[java.sql.SQLXML] = TypeBinder(_.getSQLXML)(_.getSQLXML)
  implicit val string: TypeBinder[String] = TypeBinder(_.getString)(_.getString)
  implicit val time: TypeBinder[java.sql.Time] = TypeBinder(_.getTime)(_.getTime)
  implicit val timestamp: TypeBinder[java.sql.Timestamp] = TypeBinder(_.getTimestamp)(_.getTimestamp)
  implicit val url: TypeBinder[java.net.URL] = TypeBinder(_.getURL)(_.getURL)
  implicit val dateTime: TypeBinder[DateTime] = option[java.sql.Timestamp].map(_.map(_.toDateTime).orNull[DateTime])
  implicit val localDate: TypeBinder[LocalDate] = option[java.sql.Date].map(_.map(_.toLocalDate).orNull[LocalDate])
  implicit val localTime: TypeBinder[LocalTime] = option[java.sql.Time].map(_.map(_.toLocalTime).orNull[LocalTime])

}

trait LowPriorityTypeBinderImplicits {
  implicit def option[A](implicit ev: TypeBinder[A]): TypeBinder[Option[A]] = new TypeBinder[Option[A]] {
    def apply(rs: ResultSet, columnIndex: Int): Option[A] = wrap(ev(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): Option[A] = wrap(ev(rs, columnLabel))
    private def wrap[A](a: => A): Option[A] = try Option(a) catch { case e: NullPointerException => None }
  }
}
