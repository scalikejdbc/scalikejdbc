/*
 * Copyright 2012 - 2014 scalikejdbc.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

/**
 * GlobalSettings for this library
 */
object GlobalSettings {

  /**
   * Disables specifying readOnly/autoCommit explicitly to be compatible with JTA DataSource.
   */
  var jtaDataSourceCompatible: Boolean = false

  /**
   * Enables error logging for all SQL errors.
   */
  var loggingSQLErrors: Boolean = true

  /**
   * Settings for query timing logs.
   */
  var loggingSQLAndTime: LoggingSQLAndTimeSettings = LoggingSQLAndTimeSettings()

  /**
   * Settings on SQL formatter which is used in query timing logs.
   */
  var sqlFormatter: SQLFormatterSettings = SQLFormatterSettings()

  /**
   * Settings on string-style param binding validator.
   */
  var nameBindingSQLValidator: NameBindingSQLValidatorSettings = NameBindingSQLValidatorSettings()

  /**
   * Event hanlder to be called every query completion.
   */
  var queryCompletionListener: (String, Seq[Any], Long) => Unit = (statement: String, params: Seq[Any], millis: Long) => ()

  /**
   * Event hanlder to be called every query failure.
   */
  var queryFailureListener: (String, Seq[Any], Throwable) => Unit = (statement: String, params: Seq[Any], e: Throwable) => ()

  /**
   * Event hanlder to be called every query completion when specifying tags.
   */
  var taggedQueryCompletionListener: (String, Seq[Any], Long, Seq[String]) => Unit = {
    (statement: String, params: Seq[Any], millis: Long, tags: Seq[String]) => ()
  }

  /**
   * Event hanlder to be called every query failure when specifying tags.
   */
  var taggedQueryFailureListener: (String, Seq[Any], Throwable, Seq[String]) => Unit = {
    (statement: String, params: Seq[Any], e: Throwable, tags: Seq[String]) => ()
  }

}
