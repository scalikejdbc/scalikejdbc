package scalikejdbc

import org.scalatest._
import org.mockito.Mockito._
import java.sql.ResultSet
import java.util.Calendar

import org.mockito.Matchers.{anyInt, anyString}
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar

class WrappedResultSetSpec extends FlatSpec with Matchers with MockitoSugar {

  behavior of "WrappedResultSet"

  it should "be available" in {
    val underlying: ResultSet = null
    val cursor: ResultSetCursor = new ResultSetCursor(0)
    val instance = WrappedResultSet(underlying, cursor, cursor.position)
    instance should not be null
  }

  // [NOTICE]
  // most of test cases at scalikejdbc.DBSessionSpec

  it should "have expected methods" in {

    import java.sql.{ Array => sqlArray, Blob, Clob, NClob, Ref, SQLXML, Time, Timestamp }
    import java.io.InputStream
    import java.io.Reader
    import java.util.Date
    import java.net.URL

    val underlying: ResultSet = mock[ResultSet]

    def getConditionalAnswer[Arg, Res](validArg: Arg, validRes: Res, defaultRes: Res) : Answer[_] = invocation => {
      val argument = invocation.getArguments.head.asInstanceOf[Arg]
      if (argument == validArg) {
        when(underlying.wasNull()).thenReturn(false)
        validRes
      } else {
        when(underlying.wasNull()).thenReturn(true)
        defaultRes
      }
    }

    when(underlying.getByte(anyString())).thenAnswer(getConditionalAnswer[String, Byte]("present", 1, 0))
    when(underlying.getByte(anyInt())).thenAnswer(getConditionalAnswer[Int, Byte](1, 10, 0))
    when(underlying.getShort(anyString())).thenAnswer(getConditionalAnswer[String, Short]("present", 1, 0))
    when(underlying.getShort(anyInt())).thenAnswer(getConditionalAnswer[Int, Short](1, 10, 0))
    when(underlying.getInt(anyString())).thenAnswer(getConditionalAnswer[String, Int]("present", 1, 0))
    when(underlying.getInt(anyInt())).thenAnswer(getConditionalAnswer[Int, Int](1, 10, 0))
    when(underlying.getLong(anyString())).thenAnswer(getConditionalAnswer[String, Long]("present", 1, 0))
    when(underlying.getLong(anyInt())).thenAnswer(getConditionalAnswer[Int, Long](1, 10, 0))
    when(underlying.getFloat(anyString())).thenAnswer(getConditionalAnswer[String, Float]("present", 1, 0))
    when(underlying.getFloat(anyInt())).thenAnswer(getConditionalAnswer[Int, Float](1, 10, 0))
    when(underlying.getDouble(anyString())).thenAnswer(getConditionalAnswer[String, Double]("present", 1, 0))
    when(underlying.getDouble(anyInt())).thenAnswer(getConditionalAnswer[Int, Double](1, 10, 0))

    val cursor: ResultSetCursor = new ResultSetCursor(0)
    val rs = WrappedResultSet(underlying, cursor, cursor.position)

    {
      intercept[ResultSetExtractorException] { rs.any("missing") }
      intercept[ResultSetExtractorException] { rs.any(0) }
      rs.anyOpt("missing") should be(None)
      rs.anyOpt(0) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.array("missing") }
      intercept[ResultSetExtractorException] { rs.array(0) }
      rs.arrayOpt("missing") should be(None)
      rs.arrayOpt(0) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.bigDecimal("missing") }
      intercept[ResultSetExtractorException] { rs.bigDecimal(0) }
      rs.bigDecimalOpt("missing") should be(None)
      rs.bigDecimalOpt(0) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.blob("missing") }
      intercept[ResultSetExtractorException] { rs.blob(0) }
      rs.blobOpt("missing") should be(None)
      rs.blobOpt(0) should be(None)
    }

    {
      rs.byteOpt("present").isDefined should be(true)
      rs.byteOpt("missing").isDefined should be(false)
      rs.byteOpt(0).isDefined should be(false)
      rs.byteOpt(1).isDefined should be(true)
      rs.byte("missing") should be(0.toByte) // default value
      rs.byte(0) should be(0.toByte) // default value
    }

    {
      intercept[ResultSetExtractorException] { rs.bytes("missing") }
      intercept[ResultSetExtractorException] { rs.bytes(0) }
      rs.bytesOpt("missing") should be(None)
      rs.bytesOpt(0) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.clob("missing") }
      intercept[ResultSetExtractorException] { rs.clob(0) }
      rs.clobOpt("missing") should be(None)
      rs.clobOpt(0) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.date("missing") }
      intercept[ResultSetExtractorException] { rs.date(0) }
      rs.dateOpt("missing") should be(None)
      rs.dateOpt(0) should be(None)

      intercept[ResultSetExtractorException] { rs.date("missing", Calendar.getInstance()) }
      intercept[ResultSetExtractorException] { rs.date(0, Calendar.getInstance()) }
      rs.dateOpt("missing", Calendar.getInstance()) should be(None)
      rs.dateOpt(0, Calendar.getInstance()) should be(None)
    }

    {
      rs.doubleOpt("present").isDefined should be(true)
      rs.doubleOpt("missing").isDefined should be(false)
      rs.doubleOpt(0).isDefined should be(false)
      rs.doubleOpt(1).isDefined should be(true)
      rs.double("missing") should be(0.toDouble) // default value
      rs.double(0) should be(0.toDouble) // default value
    }

    {
      rs.floatOpt("present").isDefined should be(true)
      rs.floatOpt("missing").isDefined should be(false)
      rs.floatOpt(0).isDefined should be(false)
      rs.floatOpt(1).isDefined should be(true)
      rs.float("missing") should be(0.toFloat) // default value
      rs.float(0) should be(0.toFloat) // default value
    }

    {
      rs.intOpt("present").isDefined should be(true)
      rs.intOpt("missing").isDefined should be(false)
      rs.intOpt(0).isDefined should be(false)
      rs.intOpt(1).isDefined should be(true)
      rs.int("missing") should be(0.toInt) // default value
      rs.int(0) should be(0.toInt) // default value
    }

    {
      rs.longOpt("present").isDefined should be(true)
      rs.longOpt("missing").isDefined should be(false)
      rs.longOpt(0).isDefined should be(false)
      rs.longOpt(1).isDefined should be(true)
      rs.long("missing") should be(0.toLong) // default value
      rs.long(0) should be(0.toLong) // default value
    }

    {
      intercept[ResultSetExtractorException] { rs.nClob("missing") }
      intercept[ResultSetExtractorException] { rs.nClob(0) }
      rs.nClobOpt("missing") should be(None)
      rs.nClobOpt(0) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.ref("missing") }
      intercept[ResultSetExtractorException] { rs.ref(0) }
      rs.refOpt("missing") should be(None)
      rs.refOpt(0) should be(None)
    }

    {
      rs.shortOpt("present").isDefined should be(true)
      rs.shortOpt("missing").isDefined should be(false)
      rs.shortOpt(0).isDefined should be(false)
      rs.shortOpt(1).isDefined should be(true)
      rs.short("missing") should be(0.toShort) // default value
      rs.short(0) should be(0.toShort) // default value
    }

    {
      intercept[ResultSetExtractorException] { rs.sqlXml("missing") }
      intercept[ResultSetExtractorException] { rs.sqlXml(0) }
      rs.sqlXmlOpt("missing") should be(None)
      rs.sqlXmlOpt(0) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.string("missing") }
      intercept[ResultSetExtractorException] { rs.string(0) }
      rs.stringOpt("missing") should be(None)
      rs.stringOpt(0) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.time("missing") }
      intercept[ResultSetExtractorException] { rs.time(0) }
      rs.timeOpt("missing") should be(None)
      rs.timeOpt(0) should be(None)

      intercept[ResultSetExtractorException] { rs.time("missing", Calendar.getInstance()) }
      intercept[ResultSetExtractorException] { rs.time(0, Calendar.getInstance()) }
      rs.timeOpt("missing", Calendar.getInstance()) should be(None)
      rs.timeOpt(0, Calendar.getInstance()) should be(None)
    }

    {
      intercept[ResultSetExtractorException] { rs.timestamp("missing") }
      intercept[ResultSetExtractorException] { rs.timestamp(0) }
      rs.timestampOpt("missing") should be(None)
      rs.timestampOpt(0) should be(None)

      intercept[ResultSetExtractorException] { rs.timestamp("missing", Calendar.getInstance()) }
      intercept[ResultSetExtractorException] { rs.timestamp(0, Calendar.getInstance()) }
      rs.timestampOpt("missing", Calendar.getInstance()) should be(None)
      rs.timestampOpt(0, Calendar.getInstance()) should be(None)
    }
  }

}
