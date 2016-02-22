package com.lucidchart.open.relate

import java.io.ByteArrayInputStream
import java.io.Reader
import java.net.URL
import java.sql.Blob
import java.sql.Clob
import java.sql.Clob
import java.sql.Connection
import java.sql.NClob
import java.sql.Ref
import java.sql.RowId
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp
import java.util.Calendar
import java.util.Date
import java.util.UUID
import org.specs2.mutable._
import org.specs2.mock.Mockito
import scala.collection.JavaConversions

case class TestRecord(
  id: Long,
  name: String
)

object TestRecord{
  implicit val TestRecordParseable = new Parseable[TestRecord] {
    def parse(row: SqlResult): TestRecord = TestRecord(
      row.long("id"),
      row.string("name")
    )
  }
}

class SqlResultSpec extends Specification with Mockito {
  def parser(row: SqlResult) = {
    TestRecord(
      row.long("id"),
      row.string("name")
    )
  }

  def pairparser(row: SqlResult) = {
    val id = row.long("id")
    id -> TestRecord(
      id,
      row.string("name")
    )
  }

  def getMocks = {
    val rs = mock[java.sql.ResultSet]
    (rs, SqlResult(rs))
  }

  implicit val con: Connection = null

  "asSingle" should {
    "return a single row with an explicit parser" in {
      val (rs, result) = getMocks

      rs.getRow returns 0 thenReturn 1
      rs.next returns true thenReturns false
      rs.getObject("id") returns (100L: java.lang.Long)
      rs.getObject("name") returns "the name"

      result.asSingle(parser) equals TestRecord(100L, "the name")
    }

    "return a single row with an implicit parser" in {
      val (rs, result) = getMocks

      rs.getRow returns 0 thenReturn 1
      rs.next returns true thenReturns false
      rs.getObject("id") returns (100L: java.lang.Long)
      rs.getObject("name") returns "the name"

      result.asSingle[TestRecord] equals TestRecord(100L, "the name")
    }
  }

  "asSingleOption" should {
    def init(rs: java.sql.ResultSet, next: Boolean) = {
      rs.getRow returns 0 thenReturn 1
      rs.next returns next
      rs.getObject("id") returns (100L: java.lang.Long)
      rs.getObject("name") returns "the name"
    }

    "return a single row with an explicit parser" in {
      val (rs, result) = getMocks
      init(rs, true)

      result.asSingleOption(parser) must beSome(TestRecord(100L, "the name"))
    }

    "return a None with an explicit parser" in {
      val (rs, result) = getMocks
      init(rs, false)

      result.asSingleOption(parser) must beNone
    }

    "return a single row with an implicit parser" in {
      val (rs, result) = getMocks
      init(rs, true)

      result.asSingleOption[TestRecord] must beSome(TestRecord(100L, "the name"))
    }

    "return a None with an implicit parser" in {
      val (rs, result) = getMocks
      init(rs, false)

      result.asSingleOption[TestRecord] must beNone
    }
  }

  "asList" should {
    "return a list of 3 elements with an explicit parser" in {
      val (rs, result) = getMocks

      rs.getRow returns    0 thenReturn    1 thenReturn    2 thenReturn    3
      rs.next   returns true thenReturn true thenReturn true thenReturn false
      rs.getObject("id") returns (100L: java.lang.Long)
      rs.getObject("name") returns "the name"

      result.asList(parser) equals List(TestRecord(100L, "the name"), TestRecord(100L, "the name"), TestRecord(100L, "the name"))
    }

    "return an empty list with an explicit parser" in {
      val (rs, result) = getMocks

      rs.getRow returns 0
      rs.next returns false

      result.asList(parser) equals List()
    }

    "return a list of 3 elements with an implicit parser" in {
      val (rs, result) = getMocks

      rs.getRow returns    0 thenReturn    1 thenReturn    2 thenReturn    3
      rs.next   returns true thenReturn true thenReturn true thenReturn false
      rs.getObject("id") returns (100L: java.lang.Long)
      rs.getObject("name") returns "the name"

      result.asList[TestRecord] equals List(TestRecord(100L, "the name"), TestRecord(100L, "the name"), TestRecord(100L, "the name"))
    }

    "return an empty list with an implicit parser" in {
      val (rs, result) = getMocks

      rs.getRow returns 0
      rs.next returns false

      result.asList[TestRecord] equals List()
    }
  }

  "asMap" should {
    "return a map of 3 elements with an explicit parser" in {
      val (rs, result) = getMocks
      import java.lang.{Long => L}

      rs.getRow returns    0 thenReturn    1 thenReturn    2 thenReturn    3
      rs.next   returns true thenReturn true thenReturn true thenReturn false
      rs.getObject("id") returns (1: L) thenReturns (2: L) thenReturns (3: L)
      rs.getObject("name") returns "the name"

      val res = result.asMap(pairparser)
      res(1L) equals TestRecord(1L, "the name")
      res(2L) equals TestRecord(2L, "the name")
      res(3L) equals TestRecord(3L, "the name")
    }

    "return an empty map with an explicit parser" in {
      val (rs, result) = getMocks
      rs.getRow returns 0
      rs.next returns false
      result.asMap(pairparser) equals Map()
    }

    implicit val a: Parseable[(Long, TestRecord)] = new Parseable[(Long, TestRecord)] {
      def parse(row: SqlResult) = {
        val id = row.long("id")
        id -> TestRecord(id, row.string("name"))
      }
    }

    "return a map of 3 elements with an implicit parser" in {
      val (rs, result) = getMocks
      import java.lang.{Long => L}

      rs.getRow returns    0 thenReturn    1 thenReturn    2 thenReturn    3
      rs.next   returns true thenReturn true thenReturn true thenReturn false
      rs.getObject("id") returns (1: L) thenReturns (2: L) thenReturns (3: L)
      rs.getObject("name") returns "the name"

      val res = result.asMap[Long, TestRecord]
      res(1L) equals TestRecord(1L, "the name")
      res(2L) equals TestRecord(2L, "the name")
      res(3L) equals TestRecord(3L, "the name")
    }

    "return an empty map with an implicit parser" in {
      val (rs, result) = getMocks
      rs.getRow returns 0
      rs.next returns false
      result.asMap[Long, TestRecord] equals Map()
    }
  }

  "asMultiMap" should {
    "return a multimap of 2 keys with 2 entries in each" in {
      val (rs, result) = getMocks
      import java.lang.{Long => L}

      rs.getRow returns    0 thenReturn    1 thenReturn    2 thenReturn    3 thenReturn     4
      rs.next   returns true thenReturn true thenReturn true thenReturn true thenReturn false
      rs.getObject("id") returns "1" thenReturns "2" thenReturns "1" thenReturns "2"
      rs.getObject("name") returns "one" thenReturns "two" thenReturns "three" thenReturns "four"

      val res = result.asMultiMap { row =>
        row.string("id") -> row.string("name")
      }
      res.keys must containTheSameElementsAs(Seq("1", "2"))
      res("1") must containTheSameElementsAs(Seq("one", "three"))
      res("2") must containTheSameElementsAs(Seq("two", "four"))
    }
  }

  "scalar" should {
    "return the correct type" in {
      val (rs, result) = getMocks

      rs.next returns true
      rs.getObject(1) returns (2: java.lang.Long)

      result.asScalar[Long] must_== 2L
    }

    "ignore other result values" in {
      val (rs, result) = getMocks

      rs.next returns true
      rs.getObject(1) returns ("test": java.lang.String)
      rs.getObject(2) returns (2L: java.lang.Long)

      result.asScalar[String] must_== "test"
    }

    "return null if there are no rows" in {
      val (rs, result) = getMocks

      rs.next returns false

      result.asScalarOption[Long] must_== None
    }
  }

  "extractOption" should {
    "Extract a Some" in {
      val (rs, result) = getMocks

      val name: Object = "hello"
      rs.getObject("name") returns name

      val id: Object = 12: java.lang.Integer
      rs.getObject("id") returns id


      val nameOpt = result.extractOption("name") { any =>
        any match {
          case x: String => x
          case _ => ""
        }
      }

      nameOpt must beSome("hello")

      val idOpt = result.extractOption("id") { any =>
        any match {
          case x: Int => x
          case _ => 0
        }
      }

      idOpt must beSome(12)
    }

    "Extract a None" in {
      val (rs, result) = getMocks

      rs.getObject("null") returns null

      val nullOpt = result.extractOption("null") { _ => "hello" }

      nullOpt must beNone
    }
  }

  "getRow" should {
    "return current row number of a ResultSet" in {
      val (rs, result) = getMocks

      rs.getRow() returns 3
      result.getRow equals 3
    }
  }

  "wasNull" should {
    "return true if the last read was null" in {
      val (rs, result) = getMocks

      rs.wasNull() returns true
      result.wasNull equals true
    }
  }

  "string" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res = "hello"
      rs.getObject("string") returns res
      result.string("string") equals res
      result.stringOption("string") must beSome(res)
    }
  }

  "int" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res = 10: java.lang.Integer
      rs.getInt("int") returns res
      result.int("int") equals res
      result.intOption("int") must beSome(res)
    }

    "return the correct value" in {
      val (rs, result) = getMocks

      val res = 1
      rs.getInt(1) returns res
      result.int(1) equals res
    }
  }

  "intOption" should {
    "return None if the value in the database is null" in {
      val (rs, result) = getMocks

      rs.wasNull returns true
      result.intOption("int") must beNone
    }

    "return Some(0) if the value in the database was really 0" in {
      val (rs, result) = getMocks

      val res = 0 : java.lang.Integer
      rs.getInt("int") returns res
      rs.wasNull returns false
      result.intOption("int") must beSome(res)
    }
  }

  "double" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res = 1.1: java.lang.Double
      rs.getDouble("double") returns res
      result.double("double") equals res
      result.doubleOption("double") must beSome(res)
    }
  }

  "short" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res: Short = 1
      rs.getShort("short") returns res
      result.short("short") equals res
      result.shortOption("short") must beSome(res)
    }
  }

  "byte" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res: Byte = 1
      rs.getByte("byte") returns res
      result.byte("byte") equals res
      result.byteOption("byte") must beSome(res)
    }
  }

  "boolean" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res = true
      rs.getBoolean("boolean") returns res
      result.bool("boolean") equals res
      result.boolOption("boolean") must beSome(res)
    }
  }

  "long" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res: Object = 100000L: java.lang.Long
      rs.getObject("long") returns res
      result.long("long") equals res
      result.longOption("long") must beSome(res)
    }

    "return the correct value" in {
      val (rs, result) = getMocks

      val res = 1000L
      rs.getLong(1) returns res
      result.long(1) equals res
    }
  }

  "bigInt" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val number = 1010101

      val int: Object = number.toInt: java.lang.Integer
      rs.getObject("bigInt") returns int
      result.bigInt("bigInt") equals BigInt(number)
      result.bigIntOption("bigInt") must beSome(BigInt(number))

      val long: Object = number.toLong: java.lang.Long
      rs.getObject("bigInt") returns long
      result.bigInt("bigInt") equals BigInt(number)
      result.bigIntOption("bigInt") must beSome(BigInt(number))

      val string: Object = number.toString
      rs.getObject("bigInt") returns string
      result.bigInt("bigInt") equals BigInt(number)
      result.bigIntOption("bigInt") must beSome(BigInt(number))

      val bigint: Object = new java.math.BigInteger(number.toString)
      rs.getObject("bigInt") returns bigint
      result.bigInt("bigInt") equals BigInt(number)
      result.bigIntOption("bigInt") must beSome(BigInt(number))
    }
  }

  "bigDecimal" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val number = 1.013

      val int: Object = number.toInt: java.lang.Integer
      rs.getObject("bigDecimal") returns int
      result.bigDecimal("bigDecimal") equals BigDecimal(number.toInt)
      result.bigDecimalOption("bigDecimal") must beSome(BigDecimal(number.toInt))

      val long: Object = number.toLong: java.lang.Long
      rs.getObject("bigDecimal") returns long
      result.bigDecimal("bigDecimal") equals BigDecimal(number.toLong)
      result.bigDecimalOption("bigDecimal") must beSome(BigDecimal(number.toLong))

      val string: Object = number.toString
      rs.getObject("bigDecimal") returns string
      result.bigDecimal("bigDecimal") equals BigDecimal(number)
      result.bigDecimalOption("bigDecimal") must beSome(BigDecimal(number))

      val bigint: Object = new java.math.BigDecimal(number.toString)
      rs.getObject("bigDecimal") returns bigint
      result.bigDecimal("bigDecimal") equals BigDecimal(number)
      result.bigDecimalOption("bigDecimal") must beSome(BigDecimal(number))
    }
  }

  "javaBigInteger" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val number = 1010101

      val int: Object = number.toInt: java.lang.Integer
      rs.getObject("javaBigInteger") returns int
      result.javaBigInteger("javaBigInteger") equals new java.math.BigInteger(number.toString)
      result.javaBigIntegerOption("javaBigInteger") must beSome(new java.math.BigInteger(number.toString))

      val long: Object = number.toLong: java.lang.Long
      rs.getObject("javaBigInteger") returns long
      result.javaBigInteger("javaBigInteger") equals new java.math.BigInteger(number.toString)
      result.javaBigIntegerOption("javaBigInteger") must beSome(new java.math.BigInteger(number.toString))

      val bigint: Object = new java.math.BigInteger(number.toString)
      rs.getObject("javaBigInteger") returns bigint
      result.javaBigInteger("javaBigInteger") equals new java.math.BigInteger(number.toString)
      result.javaBigIntegerOption("javaBigInteger") must beSome(new java.math.BigInteger(number.toString))
    }
  }

  "javaBigDecimal" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val number = 1

      val double: Object = number.toDouble: java.lang.Double
      rs.getObject("javaBigDecimal") returns double
      result.javaBigDecimal("javaBigDecimal") equals new java.math.BigDecimal(number.toString)
      result.javaBigDecimalOption("javaBigDecimal") must beSome(new java.math.BigDecimal(number.toString))

      val bigdec: Object = new java.math.BigDecimal(number.toString)
      rs.getObject("javaBigDecimal") returns bigdec
      result.javaBigDecimal("javaBigDecimal") equals new java.math.BigDecimal(number.toString)
      result.javaBigDecimalOption("javaBigDecimal") must beSome(new java.math.BigDecimal(number.toString))
    }
  }

  "date" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res = mock[Timestamp]
      rs.getTimestamp("date") returns res
      result.date("date") equals res
      result.dateOption("date") must beSome(res)
    }
  }

  "byteArray" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res = Array[Byte]('1','2','3')
      rs.getObject("byteArray") returns res
      result.byteArray("byteArray") equals res
      result.byteArrayOption("byteArray") must beSome(res)

      val blob = mock[Blob]
      blob.length returns res.length
      blob.getBytes(0, res.length) returns res
      rs.getObject("byteArray") returns blob
      result.byteArray("byteArray") equals res
      result.byteArrayOption("byteArray") must beSome(res)

      val clob = mock[Clob]
      clob.length returns res.length
      clob.getSubString(1, res.length) returns "123"
      rs.getObject("byteArray") returns clob
      result.byteArray("byteArray") equals res
      result.byteArrayOption("byteArray") must beSome(res)
    }
  }

  "uuid" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res = Array[Byte]('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
      rs.getObject("uuid") returns res
      result.uuid("uuid") equals new UUID(3472611983179986487L, 4051376414998685030L)
      result.uuidOption("uuid") must beSome(new UUID(3472611983179986487L, 4051376414998685030L))
    }
  }

  "uuidFromString" should {
    "return the correct value" in {
      val (rs, result) = getMocks

      val res = "000102030405060708090a0b0c0d0e0f"
      rs.getObject("uuidFromString") returns res
      result.uuidFromString("uuidFromString") equals new UUID(283686952306183L, 579005069656919567L)
      result.uuidFromStringOption("uuidFromString") must beSome(new UUID(283686952306183L, 579005069656919567L))
    }
  }

  "enum" should {
    object Things extends Enumeration {
      val one = Value(1, "one")
      val two = Value(2, "two")
      val three = Value(3, "three")
    }

    "return the correct value" in {
      val (rs, result) = getMocks

      rs.getInt("enum") returns 1 thenReturns 2 thenReturns 3 thenReturns 4
      result.enum("enum", Things) equals Things.one
      result.enum("enum", Things) equals Things.two
      result.enumOption("enum", Things) must beSome(Things.three)
      result.enumOption("enum", Things) must beNone
    }
  }
}
