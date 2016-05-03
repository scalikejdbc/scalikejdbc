package scalikejdbc

import java.io.InputStream
import java.sql.PreparedStatement

import scalikejdbc.UnixTimeInMillisConverterImplicits._
import scalikejdbc.interpolation.SQLSyntax

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

@implicitNotFound(
  "\n" +
    "--------------------------------------------------------\n" +
    " Implicit ParameterBinderFactory value fot the parameter is missing.\n" +
    " Consider wrapping the value with PrameterBindeor ParameterBinderWithValue.\n" +
    "\n" +
    "  (example1)\n" +
    "    implicit val intParameterBinderFactory: ParameterBinderFactory[Int] = ParameterBinderFactory {\n" +
    "       value => (stmt, idx) => stmt.setInt(idx, value)\n" +
    "     }\n" +
    "\n" +
    "  (example2)\n" +
    "    case class Price(value: Int)\n" +
    "    object Price {\n" +
    "      implicit val bider: TypeBinder[Price] = TypeBinder.int.map(Price.apply)\n" +
    "      implicit val unbinder: ParameterBinderFactory[Price] = ParameterBinderFactory.intParameterBinderFactory.xmap(Price.apply, _.value)\n" +
    "    }\n" +
    "\n" +
    "--------------------------------------------------------\n"
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

  implicit val intParameterBinderFactory: ParameterBinderFactory[Int] = ParameterBinderFactory { v => (ps, idx) => ps.setInt(idx, v) }
  implicit val stringParameterBinderFactory: ParameterBinderFactory[String] = ParameterBinderFactory { v => (ps, idx) => ps.setString(idx, v) }
  implicit val sqlArrayParameterBinderFactory: ParameterBinderFactory[java.sql.Array] = ParameterBinderFactory { v => (ps, idx) => ps.setArray(idx, v) }
  implicit val bigDecimalParameterBinderFactory: ParameterBinderFactory[BigDecimal] = ParameterBinderFactory { v => (ps, idx) => ps.setBigDecimal(idx, v.bigDecimal) }
  implicit val booleanParameterBinderFactory: ParameterBinderFactory[Boolean] = ParameterBinderFactory { v => (ps, idx) => ps.setBoolean(idx, v) }
  implicit val byteParameterBinderFactory: ParameterBinderFactory[Byte] = ParameterBinderFactory { v => (ps, idx) => ps.setByte(idx, v) }
  implicit val sqlDateParameterBinderFactory: ParameterBinderFactory[java.sql.Date] = ParameterBinderFactory { v => (ps, idx) => ps.setDate(idx, v) }
  implicit val doubleParameterBinderFactory: ParameterBinderFactory[Double] = ParameterBinderFactory { v => (ps, idx) => ps.setDouble(idx, v) }
  implicit val floatParameterBinderFactory: ParameterBinderFactory[Float] = ParameterBinderFactory { v => (ps, idx) => ps.setFloat(idx, v) }
  implicit val longParameterBinderFactory: ParameterBinderFactory[Long] = ParameterBinderFactory { v => (ps, idx) => ps.setLong(idx, v) }
  implicit val shortParameterBinderFactory: ParameterBinderFactory[Short] = ParameterBinderFactory { v => (ps, idx) => ps.setShort(idx, v) }
  implicit val sqlXmlParameterBinderFactory: ParameterBinderFactory[java.sql.SQLXML] = ParameterBinderFactory { v => (ps, idx) => ps.setSQLXML(idx, v) }
  implicit val sqlTimeParameterBinderFactory: ParameterBinderFactory[java.sql.Time] = ParameterBinderFactory { v => (ps, idx) => ps.setTime(idx, v) }
  implicit val sqlTimestampParameterBinderFactory: ParameterBinderFactory[java.sql.Timestamp] = ParameterBinderFactory { v => (ps, idx) => ps.setTimestamp(idx, v) }
  implicit val urlParameterBinderFactory: ParameterBinderFactory[java.net.URL] = ParameterBinderFactory { v => (ps, idx) => ps.setURL(idx, v) }
  implicit val utilDateParameterBinderFactory: ParameterBinderFactory[java.util.Date] = sqlTimestampParameterBinderFactory.xmap(identity, _.toSqlTimestamp)
  implicit val jodaDateTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.DateTime] = utilDateParameterBinderFactory.xmap(_.toJodaDateTime, _.toDate)
  implicit val jodaLocalDateTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalDateTime] = utilDateParameterBinderFactory.xmap(_.toJodaLocalDateTime, _.toDate)
  implicit val jodaLocalDateParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalDate] = sqlDateParameterBinderFactory.xmap(_.toJodaLocalDate, _.toDate.toSqlDate)
  implicit val jodaLocalTimeParameterBinderFactory: ParameterBinderFactory[org.joda.time.LocalTime] = sqlTimeParameterBinderFactory.xmap(_.toJodaLocalTime, _.toSqlTime)
  implicit val inputStreamParameterBinderFactory: ParameterBinderFactory[InputStream] = ParameterBinderFactory { v => (ps, idx) => ps.setBinaryStream(idx, v) }
  implicit val nullParameterBinderFactory: ParameterBinderFactory[Null] = new ParameterBinderFactory[Null] { def apply(value: Null) = ParameterBinder.NullParameterBinder }
  implicit val noneParameterBinderFactory: ParameterBinderFactory[None.type] = new ParameterBinderFactory[None.type] { def apply(value: None.type) = ParameterBinder.NullParameterBinder }
  implicit val sqlSyntaxParameterBinderFactory: ParameterBinderFactory[SQLSyntax] = new ParameterBinderFactory[SQLSyntax] { def apply(value: SQLSyntax) = SQLSyntaxParameterBinder(value) }
  implicit val optionalSqlSyntaxParameterBinderFactory: ParameterBinderFactory[Option[SQLSyntax]] = sqlSyntaxParameterBinderFactory.xmap(Option.apply, _ getOrElse SQLSyntax.empty)

  /**
   * Resolves already existing ParameterBinder.
   */
  implicit val parameterBinderParameterBinderFactory: ParameterBinderFactory[ParameterBinder] = new ParameterBinderFactory[ParameterBinder] {
    def apply(binder: ParameterBinder): ParameterBinderWithValue[ParameterBinder] = {
      binder match {
        case withValue: ParameterBinderWithValue[_] if withValue.value.isInstanceOf[ParameterBinder] =>
          withValue.asInstanceOf[ParameterBinderWithValue[ParameterBinder]]
        case _ =>
          new ParameterBinderWithValue[ParameterBinder] {
            override lazy val bypass = binder.bypass
            override def value: ParameterBinder = binder
            override def apply(stmt: PreparedStatement, idx: Int): Unit = {}
          }
      }
    }
  }

  /**
   * Unsafe ParameterBinderFactory which accepts any type value as-is.
   */
  val enableBypass: ParameterBinderFactory[Any] = new ParameterBinderFactory[Any] {
    def apply(value: Any) = new BypassParameterBinder(BypassParameterBinder.extractValue(value))
  }

}

trait LowPriorityImplicitsParameterBinderFactory1 extends LowPriorityImplicitsParameterBinderFactory0 {

  implicit def optionalParameterBinderFactory[A](implicit ev: ParameterBinderFactory[A]): ParameterBinderFactory[Option[A]] = new ParameterBinderFactory[Option[A]] {
    def apply(value: Option[A]): ParameterBinderWithValue[Option[A]] = {
      if (value == null) ParameterBinder.NullParameterBinder
      else value.fold[ParameterBinderWithValue[Option[A]]](ParameterBinder.NullParameterBinder)(v => ev(v).map(Option.apply))
    }
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
        c.abort(c.enclosingPosition, s"Could not find an implicit value of the ParameterBinderFactory[$A].")
    }
    c.Expr[ParameterBinderFactory[A]](expr)
  }

}
