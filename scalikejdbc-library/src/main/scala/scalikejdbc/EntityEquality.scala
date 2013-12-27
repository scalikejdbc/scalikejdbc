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
 */
trait EntityEquality {

  /**
   * Identity for this entity.
   * If you use normal class for entity, use this identity for equality.
   */
  def entityIdentity: Any

  /**
   * Predicate can be equal (see also scala.Equals).
   */
  def canEqual(that: Any): Boolean = that.getClass.isAssignableFrom(this.getClass)

  /**
   * override java.lang.Object#equals
   */
  override def equals(that: Any): Boolean = {
    if (that == null) false
    else if (!isSubClassOfThis(that)) false
    else isEntityIdentitySame(that.asInstanceOf[EntityEquality])
  }

  /**
   * override java.lang.Object#hashCode
   */
  override def hashCode: Int = entityIdentity.hashCode

  /**
   * Predicates that one is a sub-class instance of this.
   */
  private[this] def isSubClassOfThis(that: Any): Boolean = {
    this.getClass.isAssignableFrom(that.getClass)
  }

  /**
   * Predicates entity identity is same.
   */
  private[this] def isEntityIdentitySame(o: EntityEquality): Boolean = {
    o.canEqual(this) && entityIdentity == o.entityIdentity
  }

}

