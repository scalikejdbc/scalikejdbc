package scalikejdbc

/**
 * Resource manager which closes a DataSource.
 */
trait DataSourceCloser extends AutoCloseable {
  def close(): Unit
}

/**
 * Default DataSourceCloser.
 */
case object DefaultDataSourceCloser extends DataSourceCloser {
  def close(): Unit = throw new UnsupportedOperationException
}
