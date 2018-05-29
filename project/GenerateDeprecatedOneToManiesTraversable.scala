object GenerateDeprecatedOneToManiesTraversable {

  private[this] def generate: Int => String = { n =>
    val B = (1 to n).map("B" + _).mkString(", ")
    val newName = s"OneToManies${n}SQLToIterable"
    val oldName = s"OneToManies${n}SQLToTraversable"
s"""
  @deprecated(message = "use $newName instead", since = "3.3.0")
  type $oldName[A, $B, E <: WithExtractor, Z] = $newName[A, $B, E, Z]
  @deprecated(message = "use $newName instead", since = "3.3.0")
  val $oldName = $newName
"""
  }

  val value: String = {
s"""/*
 * Copyright 2013 - 2018 scalikejdbc.org
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

private[scalikejdbc] trait DeprecatedOneToManiesTraversable {
${(2 to 21).map(generate).mkString("")}
}
"""
  }

}
