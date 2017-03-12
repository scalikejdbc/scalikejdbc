package scalikejdbc

/**
 * Internally Tuner of DBSession.
 * Almost, be used by SQL object.
 */
private[scalikejdbc] final case class DBSessionInternalTuner(
    private[scalikejdbc] val fetchSize: Option[Int] = None,
    private[scalikejdbc] val tags: Seq[String] = Seq.empty,
    private[scalikejdbc] val queryTimeout: Option[Int] = None
) extends DBSessionTuner {

  private[this] var _prevFetchSize: Option[Int] = None
  private[this] var _prevTags: Seq[String] = Seq.empty
  private[this] var _prevQueryTimeout: Option[Int] = None

  override def tune(session: DBSession): session.type = {
    this._prevFetchSize = session.fetchSize
    this._prevTags = session.tags
    this._prevQueryTimeout = session.queryTimeout

    this.fetchSize.foreach(size => session.fetchSize(size))
    this.queryTimeout.foreach(seconds => session.queryTimeout(seconds))
    // Adding a tag to a session means session scope tagging.
    // So, concatenation of session.tags and this(SQL).tags would be equal to full tags.
    session.tags(session.tags ++ this.tags: _*)
  }

  override def reset(session: DBSession): session.type = {
    session
      .fetchSize(this._prevFetchSize)
      .tags(this._prevTags: _*)
      .queryTimeout(this._prevQueryTimeout)
  }
}
