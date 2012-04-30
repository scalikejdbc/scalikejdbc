package object scalikejdbc {

  type Closable = { def close() }

  def using[R <: Closable, A](resource: R)(f: R => A): A = LoanPattern.using(resource)(f)

  import java.sql.{ Timestamp => sqlTimestamp, Time => sqlTime, Date => sqlDate }
  import java.util.Date

  case class ToJavaUtilDate(t: { def getTime(): Long }) {
    def toJavaUtilDate: Date = new Date(t.getTime)
  }

  implicit def javaSqlTimestampToJavaUtilDate(t: sqlTimestamp): ToJavaUtilDate = ToJavaUtilDate(t)

  implicit def javaSqlDateToJavaUtilDate(t: sqlDate): ToJavaUtilDate = ToJavaUtilDate(t)

  implicit def javaSqlTimeToJavaUtilDate(t: sqlTime): ToJavaUtilDate = ToJavaUtilDate(t)

}

