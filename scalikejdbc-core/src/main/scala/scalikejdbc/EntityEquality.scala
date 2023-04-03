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
