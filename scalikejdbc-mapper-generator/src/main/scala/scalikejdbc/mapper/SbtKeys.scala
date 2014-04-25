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
package scalikejdbc.mapper

import sbt.{ SettingKey, InputKey }

object SbtKeys {

  lazy val scalikejdbcGen = InputKey[Unit]("scalikejdbc-gen")
  lazy val scalikejdbcGenEcho = InputKey[Unit]("scalikejdbc-gen-echo")

  lazy val scalikejdbcDriver = SettingKey[String]("scalikejdbc-driver")
  lazy val scalikejdbcUrl = SettingKey[String]("scalikejdbc-url")
  lazy val scalikejdbcUsername = SettingKey[String]("scalikejdbc-username")
  lazy val scalikejdbcPassword = SettingKey[String]("scalikejdbc-password")
  lazy val scalikejdbcSchema = SettingKey[String]("scalikejdbc-schema")
  lazy val scalikejdbcPackageName = SettingKey[String]("scalikejdbc-package-name")
  lazy val scalikejdbcLineBreak = SettingKey[String]("scalikejdbc-line-break")
  lazy val scalikejdbcCaseClassOnly = SettingKey[String]("scalikejdbc-case-class-only")

}
