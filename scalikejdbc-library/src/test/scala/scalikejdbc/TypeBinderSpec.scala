package scalikejdbc

import org.scalatest._
import mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.matchers._

class TypeBinderSpec extends FlatSpec with ShouldMatchers with MockitoSugar {

  behavior of "TypeBinder"

  it should "be able to return an Option value when NPE" in {
    val result: Option[String] = implicitly[TypeBinder[Option[String]]].apply(null, 1)
    result should be(None)
  }

}
