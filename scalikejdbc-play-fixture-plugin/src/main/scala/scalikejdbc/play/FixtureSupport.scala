/*
* Copyright 2013 Toshiyuki Takahashi
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package scalikejdbc.play

import _root_.play.api._
import java.io.File
import scala.collection.JavaConverters._
import scalikejdbc._

trait FixtureSupport {

  val fixturesRootPath: String = "db/fixtures"

  private def fixtureConfigKey(dbName: String)(implicit app: Application): String =
    if (Play.isDev) {
      "db." + dbName + ".fixtures.dev"
    } else if (Play.isTest) {
      "db." + dbName + ".fixtures.test"
    } else {
      throw new UnsupportedOperationException("Fixture feature is only provided for dev mode and test mode.")
    }

  def fixtures(implicit app: Application): Map[String, Seq[Fixture]] = {
    (for {
      dbConfig <- app.configuration.getConfig("db").toList
      subKey <- dbConfig.subKeys
    } yield {
      val dbName = subKey
      val fixtureNames: Seq[String] = try {
        app.configuration.getStringList(fixtureConfigKey(subKey))
          .map(_.asScala)
          .getOrElse(Nil)
      } catch {
        case e: PlayException => {
          app.configuration.getString(fixtureConfigKey(subKey)).toSeq
        }
      }

      val fixtureFiles = fixtureNames.map { fixtureName =>
        val resourceName = List(fixturesRootPath, dbName, fixtureName).mkString("/")
        app.resource(resourceName) match {
          case Some(resource) => Fixture(new File(resource.getPath))
          case None => throw new FixtureNotFoundException(
            "Fixture not found (%s)".format(resourceName)
          )
        }
      }

      dbName -> fixtureFiles
    }).toMap
  }

  def loadFixtures()(implicit app: Application): Unit = {
    for {
      (dbName, fs) <- fixtures
      f <- fs
    } {
      execute(dbName, f.upScript)
    }
  }

  def cleanFixtures()(implicit app: Application): Unit = {
    for {
      (dbName, fs) <- fixtures
      f <- fs.reverse
    } {
      execute(dbName, f.downScript)
    }
  }

  private def execute(dbName: String, script: String): Unit = {
    NamedDB(Symbol(dbName)) localTx { implicit session =>
      SQL(script).update.apply()
    }
  }

}
