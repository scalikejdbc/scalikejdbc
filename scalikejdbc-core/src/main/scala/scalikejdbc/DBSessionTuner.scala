package scalikejdbc

/**
 * Tune the DBSession before executing query.
 */
private[scalikejdbc] trait DBSessionTuner {

  /**
   * Tune DB session.
   *
   * SHOULD return the same session instance.
   * Also, prefer to hold session state for reset after query execution.
   *
   * @param session DB session
   * @return Same DB session as the parameter instance
   */
  def tune(session: DBSession): session.type

  /**
   * Reset DB session to previous state.
   *
   * SHOULD return the same session instance.
   *
   * @param session DB session
   * @return Same DB session as the parameter instance
   */
  def reset(session: DBSession): session.type
}

object DBSessionTuner {

  val default: DBSessionTuner = new DBSessionTuner {
    override def tune(session: DBSession): session.type = session
    override def reset(session: DBSession): session.type = session
  }
}
