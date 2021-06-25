package scalikejdbc.orm

/**
 * Pagination builder.
 */
object Pagination {

  def page(pageNo: Int): PaginationPageNoBuilder = {
    PaginationPageNoBuilder(pageNo = Option(pageNo))
  }

  def per(pageSize: Int): PaginationPageSizeBuilder = {
    PaginationPageSizeBuilder(pageSize = Option(pageSize))
  }

}

/**
 * Pagination builder.
 */
case class PaginationPageNoBuilder(pageNo: Option[Int] = None) {
  def per(pageSize: Int): Pagination =
    Pagination(pageNo = pageNo.get, pageSize = pageSize)
}

/**
 * Pagination builder.
 */
case class PaginationPageSizeBuilder(pageSize: Option[Int] = None) {
  def page(pageNo: Int): Pagination =
    Pagination(pageNo = pageNo, pageSize = pageSize.get)
}

/**
 * Pagination parameters.
 */
case class Pagination(pageSize: Int, pageNo: Int) {

  def offset: Int = (pageNo - 1) * pageSize
  def limit: Int = pageSize
}
