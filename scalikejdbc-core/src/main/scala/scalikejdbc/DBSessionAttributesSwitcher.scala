package scalikejdbc

/**
 * DBSessionAttributesSwitcher holds extra attributes came from SQL object.
 * This object can set them to the given DBSession and recover the original ones.
 */
private[scalikejdbc] class DBSessionAttributesSwitcher(sql: SQL[?, ?]) {

  // -------------------------------------
  // private attributes
  // -------------------------------------

  private[this] val fetchSizeCameFromSQLObject: Option[Int] = sql.fetchSize
  private[this] val tagsCameFromSQLObject: collection.Seq[String] = sql.tags
  private[this] val queryTimeoutCameFromSQLObject: Option[Int] =
    sql.queryTimeout

  private[this] var dbSession: Option[DBSession] = None

  private[this] var originalFetchSize: Option[Int] = None
  private[this] var originalTags: collection.Seq[String] = Seq.empty
  private[this] var originalQueryTimeout: Option[Int] = None

  // -------------------------------------
  // public methods
  // -------------------------------------

  def withSwitchedDBSession[A](session: DBSession)(op: DBSession => A): A = {
    setDBSession(session)
    overwriteAttributes()
    try {
      op(session)
    } finally {
      recoverOriginalAttributes()
    }
  }

  // ----------------------------------------

  /**
   * Saves the original attributes of a given DBSession and overwrites the attributes with the ones came from SQL object.
   */
  protected def overwriteAttributes(): Unit = {
    dbSession match {
      case Some(session) =>
        this.fetchSizeCameFromSQLObject.foreach(size => session.fetchSize(size))
        this.queryTimeoutCameFromSQLObject.foreach(seconds =>
          session.queryTimeout(seconds)
        )
        // Adding a tag to a session means session scope tagging.
        // So, concatenation of session.tags and this(SQL).tags would be equal to full tags.
        session.tags((session.tags ++ this.tagsCameFromSQLObject).toSeq: _*)
      case _ =>
        throw new IllegalStateException(ErrorMessage.THIS_IS_A_BUG)
    }
  }

  /**
   * Recovers the DBSession to have the original attributes.
   */
  protected def recoverOriginalAttributes(): Unit = {
    try {
      dbSession match {
        case Some(session) =>
          session
            .fetchSize(this.originalFetchSize)
            .tags(this.originalTags.toSeq: _*)
            .queryTimeout(this.originalQueryTimeout)
        case _ =>
          throw new IllegalStateException(ErrorMessage.THIS_IS_A_BUG)
      }
    } finally {
      resetOriginalAttributes()
    }
  }

  private[this] def setDBSession(session: DBSession): Unit = {
    dbSession match {
      case Some(_) =>
        throw new IllegalStateException(ErrorMessage.THIS_IS_A_BUG)
      case _ =>
        dbSession = Some(session)
        saveOriginalAttributes(session)
    }
  }

  private[this] def saveOriginalAttributes(session: DBSession): Unit = {
    dbSession match {
      case Some(session) =>
        this.originalFetchSize = session.fetchSize
        this.originalTags = session.tags
        this.originalQueryTimeout = session.queryTimeout
      case _ =>
        throw new IllegalStateException(ErrorMessage.THIS_IS_A_BUG)
    }
  }

  private[this] def resetOriginalAttributes(): Unit = {
    dbSession = None
    this.originalFetchSize = None
    this.originalTags = Seq.empty
    this.originalQueryTimeout = None
  }

}
