package uk.ac.warwick.tabula

class ToStringTest extends TestBase {

	private class Egg extends ToString {
		def toStringProps = Seq("id" -> 123, "name" -> "Eggy")
	}

	@Test
	def simpleTrait() {
		new Egg().toString should be ("Egg[id=123,name=Eggy]")
	}

	@Test
	def companionObject() {
		ToString.forObject(new Egg(), "id" -> 567, "hat" -> "Bowler") should be ("Egg[id=567,hat=Bowler]")
	}

}
