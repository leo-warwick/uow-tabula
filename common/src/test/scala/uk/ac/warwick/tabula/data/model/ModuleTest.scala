package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{AcademicYear, TestBase}

class ModuleTest extends TestBase {
	@Test def stripCats {
		Module.stripCats("md101-15") should be (Some("md101"))
		Module.stripCats("md105-5")  should be (Some("md105"))
		Module.stripCats("md105-7.5")  should be (Some("md105"))
		Module.stripCats("md105")  should be (Some("md105"))
		Module.stripCats("what the feck!") should be (None)
	}

	@Test def extractCats {
		Module.extractCats("md101-7.5") should be (Some("7.5"))
		Module.extractCats("md101-15") should be (Some("15"))
		Module.extractCats("md101") should be (None)
	}

	@Test def webgroupNameToModuleCode {
		Module.nameFromWebgroupName("ch-ch101") should be ("ch101")
		Module.nameFromWebgroupName("be-bo-101") should be ("bo-101")
		Module.nameFromWebgroupName("nodashes") should be ("nodashes")
	}

  @Test
  def hasUnreleasedGroupSetsReturnsTrueIfAtLeastOneSetIsUnreleased(){

		val thisAcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

    val fullyReleased = new SmallGroupSet
    fullyReleased.releasedToStudents = true
    fullyReleased.releasedToTutors = true

    val partReleased = new SmallGroupSet
    partReleased.releasedToStudents = false
    partReleased.releasedToTutors = true

		val partReleasedOld = new SmallGroupSet
		partReleasedOld.academicYear = AcademicYear(1066)
		partReleasedOld.releasedToStudents = false
		partReleasedOld.releasedToTutors = true

    val modFullyReleased = new Module()
    modFullyReleased.groupSets = Seq(fullyReleased, partReleasedOld).asJava
    modFullyReleased.hasUnreleasedGroupSets(thisAcademicYear) should be(false)
		modFullyReleased.hasUnreleasedGroupSets(AcademicYear(1066)) should be(true)

    val modPartReleased = new Module()
    modPartReleased.groupSets = Seq(fullyReleased,partReleased).asJava

    modPartReleased.hasUnreleasedGroupSets(thisAcademicYear) should be (true)
  }

	@Test
	def hasLiveAssignments(){
		val module = new Module()
		val assignment1 = new Assignment()
		assignment1.deleted = true

		val assignment2 = new Assignment()
		assignment2.archive()

		module.assignments = Seq(assignment1, assignment2).asJava
		module.hasLiveAssignments should be(false)

		assignment2.unarchive()
		module.hasLiveAssignments should be(true)
	}


}
