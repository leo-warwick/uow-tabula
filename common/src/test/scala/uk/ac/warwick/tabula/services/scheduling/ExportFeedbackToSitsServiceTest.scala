package uk.ac.warwick.tabula.services.scheduling

import java.util

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, TestBase}

class ExportFeedbackToSitsServiceTest extends TestBase {

	trait Environment {
		val year = new AcademicYear(2014)

		val module: Module = Fixtures.module("nl901", "Foraging Forays")

		val assignment: Assignment = Fixtures.assignment("Your challenge, should you choose to accept it")
		assignment.academicYear = year
		assignment.module = module
		assignment.assessmentGroups.add({
			val group = new AssessmentGroup
			group.assignment = assignment
			group.occurrence = "B"
			group.assessmentComponent = Fixtures.upstreamAssignment(Fixtures.module("nl901"), 2)
			group
		})

		val feedback: AssignmentFeedback = Fixtures.assignmentFeedback("0070790")
		feedback.assignment = assignment

		val feedbackForSits: FeedbackForSits = Fixtures.feedbackForSits(feedback, currentUser.apparentUser)

		val paramGetter = new ParameterGetter(feedbackForSits)

	}

	@Test
	def queryParams(): Unit = withUser("0070790", "cusdx") {
		new Environment {
			val inspectMe: util.HashMap[String, Object] = paramGetter.getQueryParams.get
			inspectMe.get("studentId") should be("0070790")
			inspectMe.get("academicYear") should be(year.toString)
			inspectMe.get("moduleCodeMatcher") should be("NL901%")
		}
	}

	@Test
	def noAssessmentGroups(): Unit = withUser("0070790", "cusdx") {
		new Environment {
			assignment.assessmentGroups.clear()
			val newParamGetter = new ParameterGetter(feedbackForSits)
			val inspectMe: Option[util.HashMap[String, Object]] = newParamGetter.getQueryParams
			inspectMe.isEmpty should be (true)
		}
	}

	@Test
	def updateParams(): Unit = withUser("0070790", "cusdx") {
		new Environment {

			val inspectMe: util.HashMap[String, Object] = paramGetter.getUpdateParams(73, "A").get
			inspectMe.get("studentId") should be("0070790")
			inspectMe.get("academicYear") should be(year.toString)
			inspectMe.get("moduleCodeMatcher") should be("NL901%")
			inspectMe.get("actualMark", 73)
			inspectMe.get("actualGrade", "A")
		}
	}

}
