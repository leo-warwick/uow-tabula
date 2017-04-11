package uk.ac.warwick.tabula.system

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.beans.{InvalidPropertyException, MutablePropertyValues}
import uk.ac.warwick.tabula.helpers.LazyLists
import scala.collection.JavaConversions._

class CustomDataBinderTest extends TestBase {

	@Test
	def customAutoGrowCollectionLimit {
		val cmd = new TestCommand
		val binder = new CustomDataBinder(cmd, "command") with BindListenerBinding

		val pvs = new MutablePropertyValues

		// default auto-grow collection limit is 256 - check we can exceed this
		for (count <- 0 to 256) {
			pvs.add("lazyList[" + count + "]", count.toString)
		}

		binder.bind(pvs)

		cmd.lazyList.length should be (257)

	}

	@Test(expected = classOf[InvalidPropertyException])
	def exceedsCustomAutoGrowCollectionLimit {

		val cmd = new TestCommand
		val binder = new CustomDataBinder(cmd, "command") with BindListenerBinding

		val pvs = new MutablePropertyValues
		for (count <- 0 to 10000) {
			pvs.add("lazyList[" + count + "]", count.toString)
		}

		binder.bind(pvs)
	}

	trait HasValue {
		var lazyList:JList[String] = LazyLists.create()
	}

	class TestCommand extends HasValue

}