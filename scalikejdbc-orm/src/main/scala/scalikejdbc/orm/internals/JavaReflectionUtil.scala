package scalikejdbc.orm.internals

import java.lang.reflect.Modifier

/**
 * Java reflection API utils.
 */
object JavaReflectionUtil {

  /**
   * Returns the simple name of the object's class even if invoked on the Scala REPL.
   *
   * @param obj target object
   * @return simple class name
   */
  def classSimpleName(obj: Any): String = {
    try obj.getClass.getSimpleName
    catch {
      case _: InternalError =>
        // working on the Scala REPL
        val clazz = obj.getClass
        val classOfClazz = clazz.getClass
        val getSimpleBinaryName = classOfClazz.getDeclaredMethods
          .find(_.getName == "getSimpleBinaryName")
          .get
        getSimpleBinaryName.setAccessible(true)
        getSimpleBinaryName.invoke(clazz).toString
    }
  }

  /**
   * Returns all the getter names in the Scala way.
   *
   * @param obj target object
   * @return getter names
   */
  def getterNames(obj: Any): Seq[String] = {
    val privateFieldNames = obj.getClass.getDeclaredFields.toSeq
      .filter(f =>
        Modifier.isPrivate(f.getModifiers) && !Modifier.isStatic(f.getModifiers)
      )
      .map(f => f.getName)
    val getterNames = obj.getClass.getDeclaredMethods.toSeq
      .filter(m =>
        Modifier.isPublic(m.getModifiers) && !Modifier.isStatic(m.getModifiers)
      )
      .map(m => m.getName)
    getterNames.filter(getter => privateFieldNames.contains(getter))
  }

  /**
   * Invokes getter method.
   *
   * @param obj  target object
   * @param name getter name
   * @return actual value
   */
  def getter(obj: Any, name: String): Any = {
    val clazz = obj.getClass
    val method = clazz.getDeclaredMethod(name)
    method.setAccessible(true)
    method.invoke(obj)
  }

}
