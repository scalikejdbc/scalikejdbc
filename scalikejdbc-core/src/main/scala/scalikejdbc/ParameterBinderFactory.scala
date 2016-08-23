package scalikejdbc

import java.sql.{ JDBCType, PreparedStatement, SQLType }

import scalikejdbc.interpolation.SQLSyntax

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

@implicitNotFound("Implicit ParameterBinderFactory[T] for the parameter type T is missing. You need to define ParameterBinderFactory for the type or use AsIsParameterBinder.")
trait ParameterBinderFactory[T] { self =>

  def sqlType: SQLType
  def toSqlType(value: T): Any

  def apply(value: T): ParameterBinderWithValue[T]

  def xmap[B](f: T => B, g: B => T): ParameterBinderFactory[B] = new ParameterBinderFactory[B] {
    def apply(value: B): ParameterBinderWithValue[B] = {
      if (value == null) ParameterBinder.NullParameterBinder
      else self(g(value)).map(f)
    }

    override val sqlType: SQLType = self.sqlType
    override def toSqlType(value: B) = self.toSqlType(g(value))
  }
}

object ParameterBinderFactory extends LowPriorityImplicitsParameterBinderFactory0 {

  implicit val longParameterBinderFactory: ParameterBinderFactory[Long] = Binders.long
  implicit val intParameterBinderFactory: ParameterBinderFactory[Int] = Binders.int
  implicit val shortParameterBinderFactory: ParameterBinderFactory[Short] = Binders.short
  implicit val byteParameterBinderFactory: ParameterBinderFactory[Byte] = Binders.byte
  implicit val doubleParameterBinderFactory: ParameterBinderFactory[Double] = Binders.double
  implicit val floatParameterBinderFactory: ParameterBinderFactory[Float] = Binders.float
  implicit val booleanParameterBinderFactory: ParameterBinderFactory[Boolean] = Binders.boolean
  implicit val stringParameterBinderFactory: ParameterBinderFactory[String] = Binders.string
  implicit val bigIntParameterBinderFactory: ParameterBinderFactory[BigInt] = Binders.bigInt
  implicit val bigDecimalParameterBinderFactory: ParameterBinderFactory[BigDecimal] = Binders.bigDecimal
  implicit val sqlArrayParameterBinderFactory: ParameterBinderFactory[java.sql.Array] = Binders.sqlArray
  implicit val sqlXmlParameterBinderFactory: ParameterBinderFactory[java.sql.SQLXML] = Binders.sqlXml
  implicit val sqlDateParameterBinderFactory: ParameterBinderFactory[java.sql.Date] = Binders.sqlDate
  implicit val sqlTimeParameterBinderFactory: ParameterBinderFactory[java.sql.Time] = Binders.sqlTime
  implicit val sqlTimestampParameterBinderFactory: ParameterBinderFactory[java.sql.Timestamp] = Binders.sqlTimestamp
  implicit val utilDateParameterBinderFactory: ParameterBinderFactory[java.util.Date] = Binders.utilDate
  implicit val jodaDateTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.DateTime] = Binders.jodaDateTime
  implicit val jodaLocalDateTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalDateTime] = Binders.jodaLocalDateTime
  implicit val jodaLocalDateParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalDate] = Binders.jodaLocalDate
  implicit val jodaLocalTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalTime] = Binders.jodaLocalTime
  implicit val blobParameterBinderFactory: ParameterBinderFactory[java.sql.Blob] = Binders.blob
  implicit val clobParameterBinderFactory: ParameterBinderFactory[java.sql.Clob] = Binders.clob
  implicit val nClobParameterBinderFactory: ParameterBinderFactory[java.sql.NClob] = Binders.nClob
  implicit val refParameterBinderFactory: ParameterBinderFactory[java.sql.Ref] = Binders.ref
  implicit val rowIdParameterBinderFactory: ParameterBinderFactory[java.sql.RowId] = Binders.rowId
  implicit val bytesParameterBinderFactory: ParameterBinderFactory[Array[Byte]] = Binders.bytes
  implicit val calendarParameterBinderFactory: ParameterBinderFactory[java.util.Calendar] = Binders.javaUtilCalendar

  implicit val nullParameterBinderFactory: ParameterBinderFactory[Null] = new ParameterBinderFactory[Null] {
    override def toSqlType(value: Null): Any = null
    override def apply(value: Null): ParameterBinderWithValue[Null] = new ParameterBinderWithValue[Null] {
      override def value: Null = null
      override def apply(stmt: PreparedStatement, idx: Int): Unit = stmt.setObject(idx, value)
    }
    override val sqlType: SQLType = JDBCType.OTHER
  }
  implicit val noneParameterBinderFactory: ParameterBinderFactory[None.type] = nullParameterBinderFactory.xmap(_ => None, _ => null)

  implicit val sqlSyntaxParameterBinderFactory: ParameterBinderFactory[SQLSyntax] = new ParameterBinderFactory[SQLSyntax] {
    def apply(value: SQLSyntax) = SQLSyntaxParameterBinder(value)

    // FIXME: Bogus implementation until I figure out what it is
    override val sqlType: SQLType = JDBCType.OTHER
    override def toSqlType(value: SQLSyntax): Any = value
  }

  implicit val optionalSqlSyntaxParameterBinderFactory: ParameterBinderFactory[Option[SQLSyntax]] =
    new ParameterBinderFactory[Option[SQLSyntax]] {
      def apply(value: Option[SQLSyntax]): ParameterBinderWithValue[Option[SQLSyntax]] = {
        val binder = value match {
          case null => SQLSyntaxParameterBinder(null)
          case None => SQLSyntaxParameterBinder(SQLSyntax.empty)
          case Some(syntax) => SQLSyntaxParameterBinder(syntax)
        }
        binder.map(Option.apply)
      }

      // FIXME: Bogus implementation JDBCType.OTHER I figure out what it is
      override val sqlType: SQLType = JDBCType.OTHER
      override def toSqlType(value: Option[SQLSyntax]): Any = value.fold(null.asInstanceOf[Any])(identity)
    }

  implicit def optionalParameterBinderFactory[A: ParameterBinderFactory]: ParameterBinderFactory[Option[A]] = Binders.optionWriterBinder[A]
}

trait LowPriorityImplicitsParameterBinderFactory0 {
  implicit def anyParameterBinderFactory[A]: ParameterBinderFactory[A] = macro ParameterBinderFactoryMacro.any[A]
}

private[scalikejdbc] object ParameterBinderFactoryMacro {

  def any[A: c.WeakTypeTag](c: Context): c.Expr[ParameterBinderFactory[A]] = {
    import c.universe._
    val A = weakTypeTag[A].tpe
    val expr = A.toString match {
      case "java.time.ZonedDateTime" | "java.time.OffsetDateTime" =>
        q"scalikejdbc.ParameterBinderFactory[$A] { v => (ps, idx) => ps.setTimestamp(idx, java.sql.Timestamp.from(v.toInstant)) }"
      case "java.time.LocalDateTime" =>
        q"scalikejdbc.ParameterBinderFactory[$A] { v => (ps, idx) => ps.setTimestamp(idx, java.sql.Timestamp.valueOf(v)) }"
      case "java.time.LocalDate" =>
        q"scalikejdbc.ParameterBinderFactory[$A] { v => (ps, idx) => ps.setDate(idx, java.sql.Date.valueOf(v)) }"
      case "java.time.LocalTime" =>
        q"scalikejdbc.ParameterBinderFactory[$A] { v => (ps, idx) => ps.setTime(idx, java.sql.Time.valueOf(v)) }"
      case _ =>
        c.abort(c.enclosingPosition, s"Implicit ParameterBinderFactory[$A] is missing. You need to define ParameterBinderFactory for the type or use AsIsParameterBinder.")
    }
    c.Expr[ParameterBinderFactory[A]](expr)
  }

}
