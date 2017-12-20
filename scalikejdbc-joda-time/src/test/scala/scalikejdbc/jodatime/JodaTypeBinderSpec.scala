package scalikejdbc
package jodatime

import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import java.sql.ResultSet
import org.joda.time._
import scalikejdbc.jodatime.JodaTypeBinder._

class JodaTypeBinderSpec extends FlatSpec with Matchers with MockitoSugar with UnixTimeInMillisConverterImplicits {

  behavior of "TypeBinder"

  it should "deal with optional values" in {
    val rs: ResultSet = mock[ResultSet]
    implicitly[TypeBinder[Option[DateTime]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[LocalDateTime]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[LocalDate]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[LocalTime]]].apply(rs, 1) should be(None)
  }

}
