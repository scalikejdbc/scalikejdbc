package scalikejdbc

import java.io.InputStream
import java.sql.PreparedStatement

import scalikejdbc.interpolation.SQLSyntax

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

@implicitNotFound(
  """
--------------------------------------------------------
 Implicit ParameterBinderFactory[A] for the parameter type A is missing.
 You need to define ParameterBinderFactory for the type or use AsIsParameterBinder.

  (example1)
    implicit val intParameterBinderFactory: ParameterBinderFactory[Int] = ParameterBinderFactory {
       value => (stmt, idx) => stmt.setInt(idx, value)
     }

  (example2)
    case class Price(value: Int)
    object Price {
      implicit val converter: TypeConverter[Price] = TypeConverter.int.xmap(Price.apply, _.value)
    }

  (example3)
    val value: Any = 123
    val key: SQLSyntax = sqls"column_name"
    key -> AsIsParameterBinder(value)
--------------------------------------------------------"""
)
trait ParameterBinderFactory[A] { self =>

  def apply(value: A): ParameterBinderWithValue[A]

  def xmap[B](f: A => B, g: B => A): ParameterBinderFactory[B] = new ParameterBinderFactory[B] {
    def apply(value: B): ParameterBinderWithValue[B] = {
      if (value == null) ParameterBinder.NullParameterBinder
      else self(g(value)).map(f)
    }
  }

}

object ParameterBinderFactory extends LowPriorityImplicitsParameterBinderFactory1 {

  def apply[A](f: A => (PreparedStatement, Int) => Unit): ParameterBinderFactory[A] = new ParameterBinderFactory[A] {
    def apply(value: A): ParameterBinderWithValue[A] = {
      if (value == null) ParameterBinder.NullParameterBinder
      else ParameterBinder(value, f(value))
    }
  }

  implicit val intParameterBinderFactory: ParameterBinderFactory[Int] = Binders.int
  implicit val stringParameterBinderFactory: ParameterBinderFactory[String] = Binders.string
  implicit val sqlArrayParameterBinderFactory: ParameterBinderFactory[java.sql.Array] = Binders.sqlArray
  implicit val bigDecimalParameterBinderFactory: ParameterBinderFactory[BigDecimal] = Binders.bigDecimal
  implicit val booleanParameterBinderFactory: ParameterBinderFactory[Boolean] = Binders.boolean
  implicit val byteParameterBinderFactory: ParameterBinderFactory[Byte] = Binders.byte
  implicit val sqlDateParameterBinderFactory: ParameterBinderFactory[java.sql.Date] = Binders.sqlDate
  implicit val doubleParameterBinderFactory: ParameterBinderFactory[Double] = Binders.double
  implicit val floatParameterBinderFactory: ParameterBinderFactory[Float] = Binders.float
  implicit val longParameterBinderFactory: ParameterBinderFactory[Long] = Binders.long
  implicit val shortParameterBinderFactory: ParameterBinderFactory[Short] = Binders.short
  implicit val sqlXmlParameterBinderFactory: ParameterBinderFactory[java.sql.SQLXML] = Binders.sqlXml
  implicit val sqlTimeParameterBinderFactory: ParameterBinderFactory[java.sql.Time] = Binders.sqlTime
  implicit val sqlTimestampParameterBinderFactory: ParameterBinderFactory[java.sql.Timestamp] = Binders.sqlTimestamp
  implicit val urlParameterBinderFactory: ParameterBinderFactory[java.net.URL] = Binders.url
  implicit val utilDateParameterBinderFactory: ParameterBinderFactory[java.util.Date] = Binders.utilDate
  implicit val jodaDateTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.DateTime] = Binders.jodaDateTime
  implicit val jodaLocalDateTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalDateTime] = Binders.jodaLocalDateTime
  implicit val jodaLocalDateParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalDate] = Binders.jodaLocalDate
  implicit val jodaLocalTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalTime] = Binders.jodaLocalTime
  implicit val inputStreamParameterBinderFactory: ParameterBinderFactory[InputStream] = Binders.binaryStream
  implicit val nullParameterBinderFactory: ParameterBinderFactory[Null] = new ParameterBinderFactory[Null] { def apply(value: Null) = ParameterBinder.NullParameterBinder }
  implicit val noneParameterBinderFactory: ParameterBinderFactory[None.type] = new ParameterBinderFactory[None.type] { def apply(value: None.type) = ParameterBinder.NullParameterBinder }
  implicit val sqlSyntaxParameterBinderFactory: ParameterBinderFactory[SQLSyntax] = new ParameterBinderFactory[SQLSyntax] { def apply(value: SQLSyntax) = SQLSyntaxParameterBinder(value) }
  implicit val optionalSqlSyntaxParameterBinderFactory: ParameterBinderFactory[Option[SQLSyntax]] = sqlSyntaxParameterBinderFactory.xmap(Option.apply, _ getOrElse SQLSyntax.empty)

}

trait LowPriorityImplicitsParameterBinderFactory1 extends LowPriorityImplicitsParameterBinderFactory0 {

  implicit def optionalParameterBinderFactory[A](implicit ev: ParameterBinderFactory[A]): ParameterBinderFactory[Option[A]] = new ParameterBinderFactory[Option[A]] {
    def apply(value: Option[A]): ParameterBinderWithValue[Option[A]] = {
      if (value == null) ParameterBinder.NullParameterBinder
      else if (ev == asisParameterBinderFactory) AsIsParameterBinder(value).asInstanceOf[ParameterBinderWithValue[Option[A]]]
      else value.fold[ParameterBinderWithValue[Option[A]]](ParameterBinder.NullParameterBinder)(v => ev(v).map(Option.apply))
    }
  }

  /**
   * Unsafe ParameterBinderFactory which accepts any type value as-is.
   *
   * This implicit is not enabled by default. If you need this, have implicit val definition in your own code.
   */
  val asisParameterBinderFactory: ParameterBinderFactory[Any] = new ParameterBinderFactory[Any] {
    def apply(value: Any): ParameterBinderWithValue[Any] = AsIsParameterBinder(value)
  }
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
        c.abort(c.enclosingPosition, s"""
          |--------------------------------------------------------
          | Implicit ParameterBinderFactory[$A] is missing.
          | You need to define ParameterBinderFactory for the type or use AsIsParameterBinder.
          |
          |  (example1)
          |    implicit val intParameterBinderFactory: ParameterBinderFactory[Int] = ParameterBinderFactory {
          |       value => (stmt, idx) => stmt.setInt(idx, value)
          |     }
          |
          |  (example2)
          |    case class Price(value: Int)
          |    object Price {
          |      implicit val converter: TypeConverter[Price] = TypeConverter.int.xmap(Price.apply, _.value)
          |    }
          |
          |  (example3)
          |    val value: Any = 123
          |    val key: SQLSyntax = sqls"column_name"
          |    key -> AsIsParameterBinder(value)
          |--------------------------------------------------------""".stripMargin)
    }
    c.Expr[ParameterBinderFactory[A]](expr)
  }

}
