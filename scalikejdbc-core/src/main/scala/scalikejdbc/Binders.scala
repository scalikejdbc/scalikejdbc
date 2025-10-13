package scalikejdbc

import java.io.InputStream
import java.sql.{ PreparedStatement, ResultSet }
import java.time.ZoneId
import JavaUtilDateConverterImplicits._

/**
 * Provides both of TypeBinder and ParameterBinderFactory for the specified type A.
 */
trait Binders[A] extends TypeBinder[A] with ParameterBinderFactory[A] { self =>

  def xmap[B](f: A => B, g: B => A): Binders[B] = new Binders[B] {
    def apply(rs: ResultSet, columnIndex: Int): B = f(self(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): B = f(self(rs, columnLabel))

    def apply(value: B): ParameterBinderWithValue = self.contramap(g)(value)
  }

}

/**
 * Provides factories of Binders and built-in Binders.
 */
object Binders {

  // --------------------------------------------------------------------------------------------
  // Factory methods
  // --------------------------------------------------------------------------------------------

  def apply[A](index: (ResultSet, Int) => A)(
    label: (ResultSet, String) => A
  )(f: A => (PreparedStatement, Int) => Unit): Binders[A] = new Binders[A] {
    def apply(rs: ResultSet, columnIndex: Int): A = index(rs, columnIndex)
    def apply(rs: ResultSet, columnLabel: String): A = label(rs, columnLabel)
    def apply(value: A): ParameterBinderWithValue = {
      if (value == null) ParameterBinder.NullParameterBinder
      else ParameterBinder(value, f(value))
    }
  }

  def of[A](f: Any => A)(g: A => (PreparedStatement, Int) => Unit): Binders[A] =
    new Binders[A] {
      def apply(rs: ResultSet, columnIndex: Int): A = f(
        rs.getObject(columnIndex)
      )
      def apply(rs: ResultSet, columnLabel: String): A = f(
        rs.getObject(columnLabel)
      )
      def apply(value: A): ParameterBinderWithValue = {
        if (value == null) ParameterBinder.NullParameterBinder
        else ParameterBinder(value, g(value))
      }
    }

  private[scalikejdbc] def option[A](t: Binders[A]): Binders[Option[A]] =
    option(using t, t)

  def option[A](implicit
    b: TypeBinder[A],
    p: ParameterBinderFactory[A]
  ): Binders[Option[A]] = new Binders[Option[A]] {
    def apply(rs: ResultSet, columnIndex: Int): Option[A] =
      TypeBinder.option(using b).apply(rs, columnIndex)
    def apply(rs: ResultSet, columnLabel: String): Option[A] =
      TypeBinder.option(using b).apply(rs, columnLabel)
    def apply(value: Option[A]): ParameterBinderWithValue =
      ParameterBinderFactory
        .optionalParameterBinderFactory(using p)
        .apply(value)
  }

  // ----------------------------------------------------
  // private

  private[this] def throwExceptionIfNull[A <: AnyVal, B](f: B => A)(a: B): A = {
    if (a == null) throw new UnexpectedNullValueException else f(a)
  }

  private[this] def wrapCastOption[A <: AnyVal, B](o: B): Option[A] =
    Option(o).asInstanceOf[Option[A]]

  private[this] def unwrapCastOption[A <: AnyVal, B](o: Option[A]): B =
    o match {
      case Some(v) => v.asInstanceOf[B]
      case None    => null.asInstanceOf[B]
    }

  private[scalikejdbc] def nullThrough[A, B](f: A => B)(a: A): B =
    if (a == null) null.asInstanceOf[B] else f(a)

  private[scalikejdbc] def convertJavaTimeZonedDateTime(
    z: ZoneId
  ): java.sql.Timestamp => java.time.ZonedDateTime =
    nullThrough(x => java.time.ZonedDateTime.ofInstant(x.toInstant, z))
  private[scalikejdbc] def convertJavaTimeOffsetDateTime(
    z: ZoneId
  ): java.sql.Timestamp => java.time.OffsetDateTime =
    nullThrough(x => java.time.OffsetDateTime.ofInstant(x.toInstant, z))
  private[scalikejdbc] def convertJavaTimeLocalDateTime(
    z: ZoneId
  ): java.sql.Timestamp => java.time.LocalDateTime =
    nullThrough(x => java.time.LocalDateTime.ofInstant(x.toInstant, z))

  // --------------------------------------------------------------------------------------------
  // Built-in Binders
  // --------------------------------------------------------------------------------------------

  val javaInteger: Binders[java.lang.Integer] = Binders.of[java.lang.Integer] {
    case null      => null
    case v: Float  => v.toInt.asInstanceOf[java.lang.Integer]
    case v: Double => v.toInt.asInstanceOf[java.lang.Integer]
    case n: Number => n.intValue
    case v         => java.lang.Integer.valueOf(v.toString)
  }(v => (ps, idx) => ps.setInt(idx, v.intValue))

  val int: Binders[Int] =
    javaInteger.xmap(throwExceptionIfNull(_.intValue), Integer.valueOf)
  val optionInt: Binders[Option[Int]] = javaInteger.xmap(
    wrapCastOption[Int, Integer],
    unwrapCastOption[Int, Integer]
  )

  val javaBoolean: Binders[java.lang.Boolean] = Binders.of[java.lang.Boolean] {
    case null                 => null
    case b: java.lang.Boolean => b
    case b: Boolean           => b.asInstanceOf[java.lang.Boolean]
    case s: String if s == "false" || s == "true" => s.toBoolean
    case s: String                                =>
      {
        try s.toInt != 0
        catch { case e: NumberFormatException => s.nonEmpty }
      }.asInstanceOf[java.lang.Boolean]
    case n: Number => (n.intValue() != 0).asInstanceOf[java.lang.Boolean]
    case v         => (v != 0).asInstanceOf[java.lang.Boolean]
  }(v => (ps, idx) => ps.setBoolean(idx, v))

  val boolean: Binders[Boolean] = javaBoolean.xmap(
    throwExceptionIfNull(_.booleanValue),
    java.lang.Boolean.valueOf
  )
  val optionBoolean: Binders[Option[Boolean]] = javaBoolean.xmap(
    wrapCastOption[Boolean, java.lang.Boolean],
    unwrapCastOption[Boolean, java.lang.Boolean]
  )

  val javaShort: Binders[java.lang.Short] = Binders.of[java.lang.Short] {
    case null      => null
    case v: Float  => v.toShort.asInstanceOf[java.lang.Short]
    case v: Double => v.toShort.asInstanceOf[java.lang.Short]
    case n: Number => n.shortValue
    case v         => java.lang.Short.valueOf(v.toString)
  }(v => (ps, idx) => ps.setShort(idx, v))

  val short: Binders[Short] =
    javaShort.xmap(throwExceptionIfNull(_.shortValue), java.lang.Short.valueOf)
  val optionShort: Binders[Option[Short]] = javaShort.xmap(
    wrapCastOption[Short, java.lang.Short],
    unwrapCastOption[Short, java.lang.Short]
  )

  val javaLong: Binders[java.lang.Long] = Binders.of[java.lang.Long] {
    case null      => null
    case v: Float  => v.toLong.asInstanceOf[java.lang.Long]
    case v: Double => v.toLong.asInstanceOf[java.lang.Long]
    case n: Number => n.longValue
    case v         => java.lang.Long.valueOf(v.toString)
  }(v => (ps, idx) => ps.setLong(idx, v))

  val long: Binders[Long] =
    javaLong.xmap(throwExceptionIfNull(_.longValue), java.lang.Long.valueOf)
  val optionLong: Binders[Option[Long]] = javaLong.xmap(
    wrapCastOption[Long, java.lang.Long],
    unwrapCastOption[Long, java.lang.Long]
  )

  val javaFloat: Binders[java.lang.Float] = Binders.of[java.lang.Float] {
    case null => null
    case v    => java.lang.Float.valueOf(v.toString)
  }(v => (ps, idx) => ps.setFloat(idx, v))

  val float: Binders[Float] =
    javaFloat.xmap(throwExceptionIfNull(_.floatValue), java.lang.Float.valueOf)
  val optionFloat: Binders[Option[Float]] = javaFloat.xmap(
    wrapCastOption[Float, java.lang.Float],
    unwrapCastOption[Float, java.lang.Float]
  )

  val javaDouble: Binders[java.lang.Double] = Binders.of[java.lang.Double] {
    case null => null
    case v    => java.lang.Double.valueOf(v.toString)
  }(v => (ps, idx) => ps.setDouble(idx, v))

  val double: Binders[Double] = javaDouble.xmap(
    throwExceptionIfNull(_.doubleValue),
    java.lang.Double.valueOf
  )
  val optionDouble: Binders[Option[Double]] = javaDouble.xmap(
    wrapCastOption[Double, java.lang.Double],
    unwrapCastOption[Double, java.lang.Double]
  )

  val javaByte: Binders[java.lang.Byte] = Binders.of[java.lang.Byte] {
    case null => null
    case v    => java.lang.Byte.valueOf(v.toString)
  }(v => (ps, idx) => ps.setByte(idx, v))

  val byte: Binders[Byte] =
    javaByte.xmap(throwExceptionIfNull(_.byteValue), java.lang.Byte.valueOf)
  val optionByte: Binders[Option[Byte]] = javaByte.xmap(
    wrapCastOption[Byte, java.lang.Byte],
    unwrapCastOption[Byte, java.lang.Byte]
  )

  val string: Binders[String] = Binders(_ getString _)(_ getString _)(v =>
    (ps, idx) => ps.setString(idx, v)
  )
  val sqlArray: Binders[java.sql.Array] =
    Binders(_ getArray _)(_ getArray _)(v => (ps, idx) => ps.setArray(idx, v))
  val javaBigDecimal: Binders[java.math.BigDecimal] =
    Binders(_ getBigDecimal _)(_ getBigDecimal _)(v =>
      (ps, idx) => ps.setBigDecimal(idx, v)
    )
  val bigDecimal: Binders[BigDecimal] =
    javaBigDecimal.xmap(nullThrough(BigDecimal.apply), _.bigDecimal)
  val javaBigInteger: Binders[java.math.BigInteger] = javaBigDecimal.xmap(
    nullThrough(_.toBigInteger),
    new java.math.BigDecimal(_)
  )
  val bigInt: Binders[BigInt] =
    javaBigInteger.xmap(nullThrough(BigInt.apply), _.bigInteger)
  val sqlDate: Binders[java.sql.Date] =
    Binders(_ getDate _)(_ getDate _)(v => (ps, idx) => ps.setDate(idx, v))
  val sqlXml: Binders[java.sql.SQLXML] =
    Binders(_ getSQLXML _)(_ getSQLXML _)(v =>
      (ps, idx) => ps.setSQLXML(idx, v)
    )
  val sqlTime: Binders[java.sql.Time] =
    Binders(_ getTime _)(_ getTime _)(v => (ps, idx) => ps.setTime(idx, v))
  val sqlTimestamp: Binders[java.sql.Timestamp] =
    Binders(_ getTimestamp _)(_ getTimestamp _)(v =>
      (ps, idx) => ps.setTimestamp(idx, v)
    )
  val url: Binders[java.net.URL] =
    Binders(_ getURL _)(_ getURL _)(v => (ps, idx) => ps.setURL(idx, v))
  val utilDate: Binders[java.util.Date] =
    sqlTimestamp.xmap(identity, _.toSqlTimestamp)

  val javaTimeInstant: Binders[java.time.Instant] =
    sqlTimestamp.xmap(nullThrough(_.toInstant), java.sql.Timestamp.from)
  val javaTimeZonedDateTime: Binders[java.time.ZonedDateTime] =
    sqlTimestamp.xmap(
      convertJavaTimeZonedDateTime(java.time.ZoneId.systemDefault()),
      v => java.sql.Timestamp.from(v.toInstant)
    )
  val javaTimeOffsetDateTime: Binders[java.time.OffsetDateTime] =
    sqlTimestamp.xmap(
      convertJavaTimeOffsetDateTime(java.time.ZoneId.systemDefault()),
      v => java.sql.Timestamp.from(v.toInstant)
    )
  val javaTimeLocalDateTime: Binders[java.time.LocalDateTime] =
    sqlTimestamp.xmap(
      convertJavaTimeLocalDateTime(java.time.ZoneId.systemDefault()),
      v =>
        java.sql.Timestamp.from(
          v.atZone(java.time.ZoneId.systemDefault()).toInstant
        )
    )
  val javaTimeLocalDate: Binders[java.time.LocalDate] =
    sqlDate.xmap(nullThrough(_.toLocalDate), java.sql.Date.valueOf)
  val javaTimeLocalTime: Binders[java.time.LocalTime] = sqlTime.xmap(
    nullThrough(v => {
      // java.sql.Time#toLocalTime drops its millisecond value
      val millis: Long = v.getTime
      new java.util.Date(millis).toLocalTime
    }),
    java.sql.Time.valueOf
  )

  val binaryStream: Binders[InputStream] =
    Binders(_ getBinaryStream _)(_ getBinaryStream _)(v =>
      (ps, idx) => ps.setBinaryStream(idx, v)
    )
  val blob: Binders[java.sql.Blob] =
    Binders(_ getBlob _)(_ getBlob _)(v => (ps, idx) => ps.setBlob(idx, v))
  val clob: Binders[java.sql.Clob] =
    Binders(_ getClob _)(_ getClob _)(v => (ps, idx) => ps.setClob(idx, v))
  val nClob: Binders[java.sql.NClob] =
    Binders(_ getNClob _)(_ getNClob _)(v => (ps, idx) => ps.setNClob(idx, v))
  val ref: Binders[java.sql.Ref] =
    Binders(_ getRef _)(_ getRef _)(v => (ps, idx) => ps.setRef(idx, v))
  val rowId: Binders[java.sql.RowId] =
    Binders(_ getRowId _)(_ getRowId _)(v => (ps, idx) => ps.setRowId(idx, v))
  val bytes: Binders[Array[Byte]] =
    Binders(_ getBytes _)(_ getBytes _)(v => (ps, idx) => ps.setBytes(idx, v))
  val characterStream: Binders[java.io.Reader] =
    Binders(_ getCharacterStream _)(_ getCharacterStream _)(v =>
      (ps, idx) => ps.setCharacterStream(idx, v)
    )
  val javaUtilCalendar: Binders[java.util.Calendar] = utilDate.xmap(
    nullThrough { v =>
      val c = java.util.Calendar.getInstance
      c.setTime(v)
      c
    },
    _.getTime
  )

  val asciiStream: Binders[java.io.InputStream] =
    Binders(_ getAsciiStream _)(_ getAsciiStream _)(v =>
      (ps, idx) => ps.setAsciiStream(idx, v)
    )
  val nCharacterStream: Binders[java.io.Reader] =
    Binders(_ getNCharacterStream _)(_ getNCharacterStream _)(v =>
      (ps, idx) => ps.setNCharacterStream(idx, v)
    )
  val nString: Binders[String] = Binders(_ getNString _)(_ getNString _)(v =>
    (ps, idx) => ps.setNString(idx, v)
  )

}
