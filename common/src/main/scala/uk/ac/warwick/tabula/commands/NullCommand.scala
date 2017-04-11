package uk.ac.warwick.tabula.commands

/**
 * Command that does nothing and returns nothing,
 * but you can configure it to do something for testing if you want.
 */
class NullCommand extends Command[Unit] {

	private var fn = () => {}
	def applyInternal(): Unit = {
		fn()
	}
	def will(f: () => Unit): NullCommand = {
		fn = f
		this
	}

	private var dfn = (d: Description) => {}
	def describe(d: Description) {
		dfn(d)
	}
	def describedAs(f: (Description) => Unit): NullCommand = {
		dfn = f
		this
	}

}
