package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap

class GeneratedIdTest extends TestBase{

	class Foo extends GeneratedId
	class Bar extends GeneratedId

	@Test
	def sameObjectsAreEqual(){
		val a = new Foo().tap(_.id = "test")
		a should equal(a)
	}

	@Test
	def sameClassWithSameIdAreEqual(){
			val a = new Foo().tap(_.id = "test")
		 	val b = new Foo().tap(_.id = "test")
		  a should equal(b)
	}

	@Test
	def sameClassWithDifferentIdAreNotEqual(){
		val a = new Foo().tap(_.id = "teimst")
		val b = new Foo().tap(_.id = "test2")
		a should not equal(b)
	}


	@Test
	def differentClassWithSameIdAreNotEqual(){
		val a = new Foo().tap(_.id = "test")
		val b = new Bar().tap(_.id = "test")
		a should not equal(b)
	}

	@Test
	def differentClassWithNulIdAreNotEqual(){
		val a = new Foo()
		val b = new Bar()
		a should not equal(b)
	}

	@Test
	def sameClassWithNulIdAreReferenceEqual(){
		val a = new Foo()
		val b = new Foo()
		a should equal(a)
		a should not equal(b)
	}

	// it would be nice if we could make this kind of comparison
	// be true, but we can't
	@Test
	def extendedClasssWithSameIdAreNotEqual(){
		val a = new Foo(){def x =1}.tap(_.id="test")
		val b = new Foo(){def y =2}.tap(_.id="test")
		a.getClass should not equal(b.getClass)
		a should not equal(b)


	}

}
