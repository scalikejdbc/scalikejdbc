package scalikejdbc

import java.sql.PreparedStatement
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec

class BindersSpec extends AnyFlatSpec with MockitoSugar {

  behavior of "Binders"

  // int binders
  it should "store Int values" in {
    val stmt = mock[PreparedStatement]
    Binders.int.apply(12)(stmt, 1)
    verify(stmt).setInt(1, 12)
  }

  it should "store Option[Int] value of Some as Int" in {
    val stmt = mock[PreparedStatement]
    Binders.optionInt.apply(Some(12))(stmt, 1)
    verify(stmt).setInt(1, 12)
  }

  it should "store Option[Int] value of None as null" in {
    val stmt = mock[PreparedStatement]
    Binders.optionInt.apply(None)(stmt, 1)
    verify(stmt).setObject(1, null)
  }

  // long binders
  it should "store Long values" in {
    val stmt = mock[PreparedStatement]
    Binders.long.apply(12L)(stmt, 1)
    verify(stmt).setLong(1, 12L)
  }

  it should "store Option[Long] value of Some as Long" in {
    val stmt = mock[PreparedStatement]
    Binders.optionLong.apply(Some(12L))(stmt, 1)
    verify(stmt).setLong(1, 12L)
  }

  it should "store Option[Long] value of None as null" in {
    val stmt = mock[PreparedStatement]
    Binders.optionLong.apply(None)(stmt, 1)
    verify(stmt).setObject(1, null)
  }

  // boolean binders
  it should "store Boolean values" in {
    val stmt = mock[PreparedStatement]
    Binders.boolean.apply(true)(stmt, 1)
    verify(stmt).setBoolean(1, true)
  }

  it should "store Option[Boolean] value of Some as Boolean" in {
    val stmt = mock[PreparedStatement]
    Binders.optionBoolean.apply(Some(true))(stmt, 1)
    verify(stmt).setBoolean(1, true)
  }

  it should "store Option[Boolean] value of None as null" in {
    val stmt = mock[PreparedStatement]
    Binders.optionBoolean.apply(None)(stmt, 1)
    verify(stmt).setObject(1, null)
  }

  // short binders
  it should "store Short values" in {
    val stmt = mock[PreparedStatement]
    Binders.short.apply(13)(stmt, 1)
    verify(stmt).setShort(1, 13)
  }

  it should "store Option[Short] value of Some as Short" in {
    val stmt = mock[PreparedStatement]
    Binders.optionShort.apply(Some(13))(stmt, 1)
    verify(stmt).setShort(1, 13)
  }

  it should "store Option[Short] value of None as null" in {
    val stmt = mock[PreparedStatement]
    Binders.optionShort.apply(None)(stmt, 1)
    verify(stmt).setObject(1, null)
  }

  // float binders
  it should "store Float values" in {
    val stmt = mock[PreparedStatement]
    Binders.float.apply(14.1f)(stmt, 1)
    verify(stmt).setFloat(1, 14.1f)
  }

  it should "store Option[Float] value of Some as Float" in {
    val stmt = mock[PreparedStatement]
    Binders.optionFloat.apply(Some(14.1f))(stmt, 1)
    verify(stmt).setFloat(1, 14.1f)
  }

  it should "store Option[Float] value of None as null" in {
    val stmt = mock[PreparedStatement]
    Binders.optionFloat.apply(None)(stmt, 1)
    verify(stmt).setObject(1, null)
  }

  // double binders
  it should "store Double values" in {
    val stmt = mock[PreparedStatement]
    Binders.double.apply(14.2d)(stmt, 1)
    verify(stmt).setDouble(1, 14.2d)
  }

  it should "store Option[Double] value of Some as Double" in {
    val stmt = mock[PreparedStatement]
    Binders.optionDouble.apply(Some(14.2d))(stmt, 1)
    verify(stmt).setDouble(1, 14.2d)
  }

  it should "store Option[Double] value of None as null" in {
    val stmt = mock[PreparedStatement]
    Binders.optionDouble.apply(None)(stmt, 1)
    verify(stmt).setObject(1, null)
  }

  // byte binders
  it should "store Byte values" in {
    val stmt = mock[PreparedStatement]
    Binders.byte.apply(2)(stmt, 1)
    verify(stmt).setByte(1, 2)
  }

  it should "store Option[Byte] value of Some as Byte" in {
    val stmt = mock[PreparedStatement]
    Binders.optionByte.apply(Some(2))(stmt, 1)
    verify(stmt).setByte(1, 2)
  }

  it should "store Option[Byte] value of None as null" in {
    val stmt = mock[PreparedStatement]
    Binders.optionByte.apply(None)(stmt, 1)
    verify(stmt).setObject(1, null)
  }
}
