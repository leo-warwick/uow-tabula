package uk.ac.warwick.tabula.helpers

import scala.reflect._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.util.collections.LazyList

/**
 * Lazy lists are useful in Spring forms when you have a list of rich class objects
 * because Spring won't create empty objects for you.
 *
 * e.g. myCommand.widgets[5].name=rob
 *
 * Spring will call getWidgets().get(5).setName("rob"). A LazyFactory will create an
 * empty widget at index 5 so that there isn't an NPE.
 */
object LazyLists {

	class FuncFactory[A](fn: () => A) extends LazyList.Factory[A] {
		override def create: A = fn()
	}

	/**
	 * Creates a lazy list (optionally based on an existing list) that will create
	 * elements on demand using the default no-arg constructor for the class.
	 */
	def create[A: ClassTag](list: JList[A] = JArrayList[A]()): JList[A] = {
		val constructor = classTag[A].runtimeClass.getDeclaredConstructor()
		val factory = () => constructor.newInstance().asInstanceOf[A]

		createWithFactory[A](factory, list)
	}

	def createWithFactory[A](fn: () => A, list: JList[A] = JArrayList[A]()): JList[A] =
		LazyList.decorate(list, new FuncFactory(fn)).asInstanceOf[JList[A]]

}