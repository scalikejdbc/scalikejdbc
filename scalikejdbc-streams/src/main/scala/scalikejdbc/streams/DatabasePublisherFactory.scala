package scalikejdbc.streams

/**
 * Factory of DatabasePublisher.
 */
private[streams] object DatabasePublisherFactory {

  /**
   * Creates and returns new DatabasePublisher instance.
   */
  private[streams] def createNewPublisher[A](
    publisherSettings: DatabasePublisherSettings[A],
    asyncExecutor: AsyncExecutor,
    sql: StreamReadySQL[A]
  ): DatabasePublisher[A] = {

    new DatabasePublisher[A](publisherSettings, sql, asyncExecutor)
  }

}
