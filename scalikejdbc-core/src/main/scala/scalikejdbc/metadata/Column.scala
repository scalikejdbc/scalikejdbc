/*
 * Copyright 2011 - 2015 scalikejdbc.org
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
package scalikejdbc.metadata

/**
 * Column meta data
 *
 * @param name name
 * @param typeCode type code(int)
 * @param typeName type name
 * @param size size
 * @param isRequired not null
 * @param isPrimaryKey primary key
 * @param isAutoIncrement auto increment
 * @param description comment
 * @param defaultValue default value
 */
case class Column(name: String,
  typeCode: Int,
  typeName: String,
  size: Int = 0,
  isRequired: Boolean = false,
  isPrimaryKey: Boolean = false,
  isAutoIncrement: Boolean = false,
  description: String = null,
  defaultValue: String = null)
