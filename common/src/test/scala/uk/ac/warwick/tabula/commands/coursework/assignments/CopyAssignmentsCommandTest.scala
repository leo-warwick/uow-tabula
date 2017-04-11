package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.data.model.forms.{MarkerSelectField, WordCountField}

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model._
import org.joda.time.DateTime

import collection.JavaConverters._

class CopyAssignmentsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends AssessmentServiceComponent with AssessmentMembershipServiceComponent {
		val assessmentService: AssessmentService = smartMock[AssessmentService]
		val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
		def apply(): Seq[Assignment] = Seq()
	}

	trait Fixture {
		val department: Department = Fixtures.department("bs")
		val module: Module = Fixtures.module("bs101")
		module.adminDepartment = department

		val fakeDate = new DateTime(2013, 8, 23, 0, 0)

		val assignment: Assignment = Fixtures.assignment("Test")
		assignment.addDefaultFields()
		assignment.academicYear = AcademicYear.parse("12/13")
		assignment.module = module
		assignment.openDate = fakeDate
		assignment.closeDate = fakeDate.plusDays(30)
		assignment.openEnded = false
		assignment.collectMarks = true
		assignment.collectSubmissions = true
		assignment.restrictSubmissions = true
		assignment.allowLateSubmissions = true
		assignment.allowResubmission = false
		assignment.displayPlagiarismNotice = true
		assignment.allowExtensions = true
		assignment.extensionAttachmentMandatory = true
		assignment.allowExtensionsAfterCloseDate = true
		assignment.summative = false
		assignment.assignmentService = smartMock[AssessmentService]
	}

	@Test
	def commandApply() {
		new Fixture {
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)
			command.archive = true
			val newAssignment: Assignment = command.applyInternal().get(0)

			verify(command.assessmentService, times(1)).save(assignment)
			verify(command.assessmentService, times(1)).save(newAssignment)
		}
	}

	@Test
	def copy() {
		new Fixture with FindAssignmentFields {
			withFakeTime(fakeDate) {
				val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
				command.assignments = Seq(assignment)
				command.archive = true
				val newAssignment = command.applyInternal().get(0)

				assignment.isAlive should be {false}
				newAssignment.academicYear.toString should be("13/14")
				newAssignment.module should be(module)
				newAssignment.name should be("Test")
				newAssignment.openDate should be(new DateTime(2014, 8, 22, 0, 0))
				newAssignment.closeDate should be(new DateTime(2014, 8, 22, 0, 0).plusDays(30))
				newAssignment.openEnded.booleanValue should be {false}
				newAssignment.collectMarks.booleanValue should be {true}
				newAssignment.collectSubmissions.booleanValue should be {true}
				newAssignment.restrictSubmissions.booleanValue should be {true}
				newAssignment.allowLateSubmissions.booleanValue should be {true}
				newAssignment.allowResubmission.booleanValue should be {false}
				newAssignment.displayPlagiarismNotice.booleanValue should be {true}
				newAssignment.allowExtensions.booleanValue should be {true}
				newAssignment.extensionAttachmentMandatory.booleanValue should be {true}
				newAssignment.allowExtensionsAfterCloseDate.booleanValue should be {true}
				newAssignment.summative.booleanValue should be {false}
			}
		}
	}

	@Test def guessSitsLinks() {
		new Fixture {
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)
			command.academicYear = AcademicYear.parse("13/14")

			val ag1: AssessmentGroup = {
				val group = new AssessmentGroup
				group.assignment = assignment
				group.occurrence = "A"
				group.assessmentComponent = Fixtures.upstreamAssignment(Fixtures.module("bs101"), 1)
				group.membershipService = command.assessmentMembershipService
				group
			}

			val ag2: AssessmentGroup = {
				val group = new AssessmentGroup
				group.assignment = assignment
				group.occurrence = "B"
				group.assessmentComponent = Fixtures.upstreamAssignment(Fixtures.module("bs102"), 2)
				group.membershipService = command.assessmentMembershipService
				group
			}

			assignment.assessmentGroups.add(ag1)
			assignment.assessmentGroups.add(ag2)

			val template1: UpstreamAssessmentGroup = {
				val template = new UpstreamAssessmentGroup
				template.academicYear = AcademicYear.parse("13/14")
				template.assessmentGroup = ag1.assessmentComponent.assessmentGroup
				template.moduleCode = ag1.assessmentComponent.moduleCode
				template.occurrence = ag1.occurrence
				template
			}
			val template2: UpstreamAssessmentGroup = {
				val template = new UpstreamAssessmentGroup
				template.academicYear = AcademicYear.parse("13/14")
				template.assessmentGroup = ag2.assessmentComponent.assessmentGroup
				template.moduleCode = ag2.assessmentComponent.moduleCode
				template.occurrence = ag2.occurrence
				template
			}

			command.assessmentMembershipService.getUpstreamAssessmentGroup(any[UpstreamAssessmentGroup]) answers { t =>
				val template = t.asInstanceOf[UpstreamAssessmentGroup]
				if (template.occurrence == "A")
					Some(Fixtures.assessmentGroup(template1.academicYear, ag1.assessmentComponent.assessmentGroup, ag1.assessmentComponent.moduleCode, ag1.occurrence))
				else
					None
			}

			val newAssignment: Assignment = command.applyInternal().get(0)
			newAssignment.assessmentGroups.size should be (1)

			val link: AssessmentGroup = newAssignment.assessmentGroups.get(0)
			link.assessmentComponent should be (ag1.assessmentComponent)
			link.assignment should be (newAssignment)
			link.occurrence should be (ag1.occurrence)
		}
	}

	@Test
	def copyDefaultFields() {
		new Fixture with FindAssignmentFields {
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)
			val newAssignment: Assignment = command.applyInternal().get(0)

			findCommentField(newAssignment).get.value should be ("")
			findFileField(newAssignment).get.attachmentLimit should be (1)
			findFileField(newAssignment).get.attachmentTypes should be (Nil)
			findWordCountField(newAssignment).isEmpty should be {true}
		}
	}

	@Test
	def copyFieldValues() {
		new Fixture with FindAssignmentFields {

			val heronRant = "Words describing the evil nature of Herons will not count towards the final word count. Herons are scum. Hate them!"
			val wordCountField = new WordCountField
			wordCountField.assignment = assignment
			wordCountField.name = Assignment.defaultWordCountName
			wordCountField.max = 5000
			wordCountField.min = 4500
			wordCountField.conventions = heronRant
			assignment.addField(wordCountField)
			val extremeHeronRant: String = heronRant.replace("Hate them", "Spit at them!")
			findCommentField(assignment).get.value = extremeHeronRant
			findFileField(assignment).get.attachmentLimit = 9999
			findFileField(assignment).get.attachmentTypes = Seq(".hateherons")
			findFileField(assignment).get.individualFileSizeLimit = 100
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)
			val newAssignment: Assignment = command.applyInternal().get(0)

			findCommentField(newAssignment).get.value should be (extremeHeronRant)
			findFileField(newAssignment).get.attachmentLimit should be (9999)
			findFileField(newAssignment).get.attachmentTypes should be (Seq(".hateherons"))
			findFileField(newAssignment).get.individualFileSizeLimit should be (100)
			findWordCountField(newAssignment).get.max should be(5000)
			findWordCountField(newAssignment).get.min should be(4500)
			findWordCountField(newAssignment).get.conventions should be(heronRant)
		}
	}

	// TAB-3521
	@Test
	def copyMarkerField() {
		new Fixture with FindAssignmentFields {
			val studentChooseWorkflow = new OldStudentsChooseMarkerWorkflow
			assignment.markingWorkflow = studentChooseWorkflow
			assignment.addField(new MarkerSelectField)
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)

			val newAssignment: Assignment = command.applyInternal().get(0)
			newAssignment.fields.asScala.exists{
				case f: MarkerSelectField => true
					case _ => false
			} should be {true}
			newAssignment.fields.asScala.exists{
				case f: WordCountField => true
				case _ => false
			} should be {false}
		}
	}

}