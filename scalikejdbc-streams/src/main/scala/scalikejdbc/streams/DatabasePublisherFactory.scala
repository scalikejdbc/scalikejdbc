package scalikejdbc.streams

/**
 * Factory of DatabasePublisher.
 */
object DatabasePublisherFactory {

  def createNewPublisher[A](
    publisherSettings: DatabasePublisherSettings[A],
    asyncExecutor: AsyncExecutor,
    sql: StreamSQL[A]
  ): DatabasePublisher[A] = {
    new DatabasePublisher[A](
      publisherSettings,
      sql,
      asyncExecutor,
      new StreamEmitter[A]()
    )
  }

}
