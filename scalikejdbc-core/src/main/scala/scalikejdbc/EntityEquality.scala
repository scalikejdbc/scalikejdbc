/*
 * Copyright 2013 Kazuhiro Sera, Manabu Nakamura
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
 * Entity identifier provider for equality (especially for scalikejdbc.RelationalSQL operation).
 *
 * Notice: Inheritance is not supported.
 *
 * @example {{{
 *   class Person(val id: Long) extends EntityEquality { override val entityIdentity = id }
 *   class Member(override val id: Long) extends Person(id)
 *
 *   val p1 = new Person(123)
 *   val p2 = new Person(123)
 *   val m1 = new Member(123)
 *   val m2 = new Member(123)
 *
 *   p1 == p2 && p2 == p1 // true
 *   p1 == m1 || m1 == p1 // false
 *   m1 == m2 && m2 == m1 // true
 * }}}
 */
trait EntityEquality {

  /**
   * Identity for this entity.
   * If you use normal class for entity, use this identity for equality.
   */
  def entityIdentity: Any

  /**
   * override java.lang.Object#equals
   */
  override def equals(that: Any): Boolean = {
    if (that == null) false
    else if (!that.isInstanceOf[EntityEquality]) false
    else isEntityIdentitySame(that.asInstanceOf[EntityEquality])
  }

  /**
   * override java.lang.Object#hashCode
   */
  override def hashCode: Int = entityIdentity.hashCode

  /**
   * Predicates entity identity is same.
   */
  private[this] def isEntityIdentitySame(that: EntityEquality): Boolean = {
    this.getClass == that.getClass && this.entityIdentity == that.entityIdentity
  }

}

