package util

import org.slf4j.LoggerFactory
import scalikejdbc._

/**
  * DBSeeds runner.
  */
class DBSeedsRunner extends DBSeeds

/**
  * Seeds database tables or records instantly.
  *
  * This module is surely inspired by Rails rake db:seed.
  */
trait DBSeeds {

  private[this] val logger = LoggerFactory.getLogger(classOf[DBSeeds])

  /**
    * AutoSession for this.
    */
  val dbSeedsAutoSession: DBSession = AutoSession

  /**
    * Registered operations.
    */
  private[this] val registeredSeedOperations =
    new collection.mutable.ListBuffer[() => Any]

  /**
    * Adds new SQLs to execute when #run is called.
    *
    * @param seedSQLs seed SQLs
    * @param session db session
    * @return self
    */
  def addSeedSQL(
    seedSQLs: SQL[?, ?]*
  )(implicit session: DBSession = dbSeedsAutoSession): DBSeeds = {
    registeredSeedOperations ++= seedSQLs.map(s => () => s.execute.apply())
    this
  }

  /**
    * Adds seed operation to execute when #run is called.
    *
    * @param op operation
    * @return self
    */
  def addSeed(op: => Any): DBSeeds = {
    registeredSeedOperations.append(() => op)
    this
  }

  /**
    * Runs if predicate function returns false.
    *
    * @param predicate predicate function
    * @param session db session
    * @return nothing
    */
  def runUnless(
    predicate: => Boolean
  )(implicit session: DBSession = dbSeedsAutoSession): Unit = {
    ConnectionPool.synchronized {
      if (!predicate) {
        logger.info(
          s"Since #runUnless predication failed, DBSeeds is going to run now."
        )
        run()
      }
    }
  }

  /**
    * Runs if SQL execution failed.
    * @return nothing
    */
  def runIfFailed(
    sql: SQL[?, ?]
  )(implicit session: DBSession = dbSeedsAutoSession): Unit = {
    ConnectionPool.synchronized {
      try sql.execute.apply()
      catch {
        case _: java.sql.SQLException =>
          logger.info(
            s"Since '${sql.statement}' execution failed, DBSeeds is going to run now."
          )
          run()
      }
    }
  }

  /**
    * Run all the seeds.
    *
    * @param session db session
    * @return nothing
    */
  def run()(implicit session: DBSession = dbSeedsAutoSession): Unit = {
    ConnectionPool.synchronized {
      registeredSeedOperations.foreach(_.apply())
    }
  }

}
