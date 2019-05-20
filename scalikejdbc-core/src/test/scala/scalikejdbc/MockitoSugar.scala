package scalikejdbc

import org.mockito.Mockito
import scala.reflect.ClassTag

trait MockitoSugar {
  def mock[T <: AnyRef](implicit c: ClassTag[T]): T =
    Mockito.mock(c.runtimeClass.asInstanceOf[Class[T]])
}
