/*
 * Copyright 2012 Kazuhiro Sera
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

  var loggingSQLErrors: Boolean = true

  var loggingSQLAndTime: LoggingSQLAndTimeSettings = LoggingSQLAndTimeSettings()

  var sqlFormatter: SQLFormatterSettings = SQLFormatterSettings()

  var nameBindingSQLValidator: NameBindingSQLValidatorSettings = NameBindingSQLValidatorSettings()

  var queryCompletionListener: (String, Seq[Any], Long) => Unit = (statement: String, params: Seq[Any], millis: Long) => ()

  var queryFailureListener: (String, Seq[Any], Throwable) => Unit = (statement: String, params: Seq[Any], e: Throwable) => ()

  var taggedQueryCompletionListener: (String, Seq[Any], Long, Seq[String]) => Unit = {
    (statement: String, params: Seq[Any], millis: Long, tags: Seq[String]) => ()
  }

  var taggedQueryFailureListener: (String, Seq[Any], Throwable, Seq[String]) => Unit = {
    (statement: String, params: Seq[Any], e: Throwable, tags: Seq[String]) => ()
  }

}
