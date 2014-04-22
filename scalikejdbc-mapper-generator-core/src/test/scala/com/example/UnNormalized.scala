package com.example

import scalikejdbc._
import org.joda.time.{ LocalDate, LocalTime, DateTime }

class UnNormalized(
    val id: Long,
    val v01: Byte,
    val v02: Short,
    val v03: Int,
    val v04: Long,
    val v05: BigDecimal,
    val v06: BigDecimal,
    val v07: Double,
    val v08: Option[Boolean] = None,
    val v09: Option[String] = None,
    val v10: String,
    val v11: Option[Byte] = None,
    val v12: Option[Short] = None,
    val v13: Option[Int] = None,
    val v14: Option[Long] = None,
    val v15: Option[BigDecimal] = None,
    val v16: Option[Boolean] = None,
    val v17: LocalDate,
    val v18: LocalTime,
    val v19: LocalTime,
    val v20: DateTime,
    val v21: Option[String] = None,
    val v22: Boolean,
    val v23: Float,
    val v24: Double,
    val createdAt: DateTime) {

  def copy(
    id: Long = this.id,
    v01: Byte = this.v01,
    v02: Short = this.v02,
    v03: Int = this.v03,
    v04: Long = this.v04,
    v05: BigDecimal = this.v05,
    v06: BigDecimal = this.v06,
    v07: Double = this.v07,
    v08: Option[Boolean] = this.v08,
    v09: Option[String] = this.v09,
    v10: String = this.v10,
    v11: Option[Byte] = this.v11,
    v12: Option[Short] = this.v12,
    v13: Option[Int] = this.v13,
    v14: Option[Long] = this.v14,
    v15: Option[BigDecimal] = this.v15,
    v16: Option[Boolean] = this.v16,
    v17: LocalDate = this.v17,
    v18: LocalTime = this.v18,
    v19: LocalTime = this.v19,
    v20: DateTime = this.v20,
    v21: Option[String] = this.v21,
    v22: Boolean = this.v22,
    v23: Float = this.v23,
    v24: Double = this.v24,
    createdAt: DateTime = this.createdAt): UnNormalized = {
    new UnNormalized(
      id = id,
      v01 = v01,
      v02 = v02,
      v03 = v03,
      v04 = v04,
      v05 = v05,
      v06 = v06,
      v07 = v07,
      v08 = v08,
      v09 = v09,
      v10 = v10,
      v11 = v11,
      v12 = v12,
      v13 = v13,
      v14 = v14,
      v15 = v15,
      v16 = v16,
      v17 = v17,
      v18 = v18,
      v19 = v19,
      v20 = v20,
      v21 = v21,
      v22 = v22,
      v23 = v23,
      v24 = v24,
      createdAt = createdAt)
  }

  def save()(implicit session: DBSession = UnNormalized.autoSession): UnNormalized = UnNormalized.save(this)(session)

  def destroy()(implicit session: DBSession = UnNormalized.autoSession): Unit = UnNormalized.destroy(this)(session)

}

object UnNormalized extends SQLSyntaxSupport[UnNormalized] {

  override val tableName = "UN_NORMALIZED"

  override val columns = Seq("ID", "V_01", "V_02", "V_03", "V_04", "V_05", "V_06", "V_07", "V_08", "V_09", "V_10", "V_11", "V_12", "V_13", "V_14", "V_15", "V_16", "V_17", "V_18", "V_19", "V_20", "V_21", "V_22", "V_23", "V_24", "CREATED_AT")

  def apply(un: SyntaxProvider[UnNormalized])(rs: WrappedResultSet): UnNormalized = apply(un.resultName)(rs)
  def apply(un: ResultName[UnNormalized])(rs: WrappedResultSet): UnNormalized = new UnNormalized(
    id = rs.get(un.id),
    v01 = rs.get(un.v01),
    v02 = rs.get(un.v02),
    v03 = rs.get(un.v03),
    v04 = rs.get(un.v04),
    v05 = rs.get(un.v05),
    v06 = rs.get(un.v06),
    v07 = rs.get(un.v07),
    v08 = rs.get(un.v08),
    v09 = rs.get(un.v09),
    v10 = rs.get(un.v10),
    v11 = rs.get(un.v11),
    v12 = rs.get(un.v12),
    v13 = rs.get(un.v13),
    v14 = rs.get(un.v14),
    v15 = rs.get(un.v15),
    v16 = rs.get(un.v16),
    v17 = rs.get(un.v17),
    v18 = rs.get(un.v18),
    v19 = rs.get(un.v19),
    v20 = rs.get(un.v20),
    v21 = rs.get(un.v21),
    v22 = rs.get(un.v22),
    v23 = rs.get(un.v23),
    v24 = rs.get(un.v24),
    createdAt = rs.get(un.createdAt)
  )

  val un = UnNormalized.syntax("un")

  override val autoSession = AutoSession

  def find(id: Long)(implicit session: DBSession = autoSession): Option[UnNormalized] = {
    withSQL {
      select.from(UnNormalized as un).where.eq(un.id, id)
    }.map(UnNormalized(un.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[UnNormalized] = {
    withSQL(select.from(UnNormalized as un)).map(UnNormalized(un.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(UnNormalized as un)).map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[UnNormalized] = {
    withSQL {
      select.from(UnNormalized as un).where.append(sqls"${where}")
    }.map(UnNormalized(un.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls"count(1)").from(UnNormalized as un).where.append(sqls"${where}")
    }.map(_.long(1)).single.apply().get
  }

  def create(
    v01: Byte,
    v02: Short,
    v03: Int,
    v04: Long,
    v05: BigDecimal,
    v06: BigDecimal,
    v07: Double,
    v08: Option[Boolean] = None,
    v09: Option[String] = None,
    v10: String,
    v11: Option[Byte] = None,
    v12: Option[Short] = None,
    v13: Option[Int] = None,
    v14: Option[Long] = None,
    v15: Option[BigDecimal] = None,
    v16: Option[Boolean] = None,
    v17: LocalDate,
    v18: LocalTime,
    v19: LocalTime,
    v20: DateTime,
    v21: Option[String] = None,
    v22: Boolean,
    v23: Float,
    v24: Double,
    createdAt: DateTime)(implicit session: DBSession = autoSession): UnNormalized = {
    val generatedKey = withSQL {
      insert.into(UnNormalized).columns(
        column.v01,
        column.v02,
        column.v03,
        column.v04,
        column.v05,
        column.v06,
        column.v07,
        column.v08,
        column.v09,
        column.v10,
        column.v11,
        column.v12,
        column.v13,
        column.v14,
        column.v15,
        column.v16,
        column.v17,
        column.v18,
        column.v19,
        column.v20,
        column.v21,
        column.v22,
        column.v23,
        column.v24,
        column.createdAt
      ).values(
          v01,
          v02,
          v03,
          v04,
          v05,
          v06,
          v07,
          v08,
          v09,
          v10,
          v11,
          v12,
          v13,
          v14,
          v15,
          v16,
          v17,
          v18,
          v19,
          v20,
          v21,
          v22,
          v23,
          v24,
          createdAt
        )
    }.updateAndReturnGeneratedKey.apply()

    new UnNormalized(
      id = generatedKey,
      v01 = v01,
      v02 = v02,
      v03 = v03,
      v04 = v04,
      v05 = v05,
      v06 = v06,
      v07 = v07,
      v08 = v08,
      v09 = v09,
      v10 = v10,
      v11 = v11,
      v12 = v12,
      v13 = v13,
      v14 = v14,
      v15 = v15,
      v16 = v16,
      v17 = v17,
      v18 = v18,
      v19 = v19,
      v20 = v20,
      v21 = v21,
      v22 = v22,
      v23 = v23,
      v24 = v24,
      createdAt = createdAt)
  }

  def save(entity: UnNormalized)(implicit session: DBSession = autoSession): UnNormalized = {
    withSQL {
      update(UnNormalized).set(
        column.id -> entity.id,
        column.v01 -> entity.v01,
        column.v02 -> entity.v02,
        column.v03 -> entity.v03,
        column.v04 -> entity.v04,
        column.v05 -> entity.v05,
        column.v06 -> entity.v06,
        column.v07 -> entity.v07,
        column.v08 -> entity.v08,
        column.v09 -> entity.v09,
        column.v10 -> entity.v10,
        column.v11 -> entity.v11,
        column.v12 -> entity.v12,
        column.v13 -> entity.v13,
        column.v14 -> entity.v14,
        column.v15 -> entity.v15,
        column.v16 -> entity.v16,
        column.v17 -> entity.v17,
        column.v18 -> entity.v18,
        column.v19 -> entity.v19,
        column.v20 -> entity.v20,
        column.v21 -> entity.v21,
        column.v22 -> entity.v22,
        column.v23 -> entity.v23,
        column.v24 -> entity.v24,
        column.createdAt -> entity.createdAt
      ).where.eq(column.id, entity.id)
    }.update.apply()
    entity
  }

  def destroy(entity: UnNormalized)(implicit session: DBSession = autoSession): Unit = {
    withSQL { delete.from(UnNormalized).where.eq(column.id, entity.id) }.update.apply()
  }

}
