package uk.ac.warwick.tabula

import org.mockito.verification.VerificationMode
import org.specs2.matcher.Matchers._
import scala.reflect.ClassTag

trait Mockito extends org.specs2.mock.Mockito {
  def isEq[A](arg:A): A = argThat(equalTo(arg)).asInstanceOf[A]
  def isNotEq[A](arg:A): A = argThat(not(equalTo(arg))).asInstanceOf[A]
	def isA[A](implicit tag: ClassTag[A]): A = org.mockito.Matchers.isA(tag.runtimeClass.asInstanceOf[Class[A]])
	def isNull[A](implicit tag: ClassTag[A]): A = org.mockito.Matchers.isNull(tag.runtimeClass.asInstanceOf[Class[A]])
	def reset[A](arg:A): Unit = org.mockito.Mockito.reset(arg)

	def verifyNoMoreInteractions(mocks: AnyRef*): Unit = org.mockito.Mockito.verifyNoMoreInteractions(mocks : _*)
	def verify[A](mock: A, mode: VerificationMode): A = org.mockito.Mockito.verify(mock, mode)
	def verify[A](mock: A): A = org.mockito.Mockito.verify(mock)
	def times(arg: Int): VerificationMode = org.mockito.Mockito.times(arg)
	def never(): VerificationMode = org.mockito.Mockito.never()
	def atLeast(arg: Int): VerificationMode = org.mockito.Mockito.atLeast(arg)
	def atMost(arg: Int): VerificationMode = org.mockito.Mockito.atMost(arg)

	override def there = throw new UnsupportedOperationException("Can't use specs2 expectations. See TAB-3390")
}