package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.tabula.data.model.{GradeBoundary, Module}
import uk.ac.warwick.tabula.web.controllers.AbstractGenerateGradeFromMarkController.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.{Mockito, TestBase}

class AbstractGenerateGradeFromMarkControllerTest extends TestBase with Mockito {

	val cmd = new AbstractGenerateGradeFromMarkController {
		override def command(module: Module, assessment: Nothing): GenerateGradesFromMarkCommand = null
	}

	val gradeBoundary = GradeBoundary(null, "B", 0, 100, "N")
	val gradeBoundaryNoDefault = GradeBoundary(null, "B", 0, 100, "Y")

	@Test
	def checkDefaultValidSelected(): Unit = {
		val result = cmd.defaultGrade("1234", Map("1234" -> "60"), Map("1234" -> Seq(gradeBoundary)), Map("1234" -> "B"))
		result.isDefined should be {true}
		result.get should be (gradeBoundary)
	}

	@Test
	def checkDefaultInvalidSelected(): Unit = {
		val result = cmd.defaultGrade("1234", Map("1234" -> "60"), Map("1234" -> Seq(gradeBoundary)), Map("1234" -> "F"))
		result.isDefined should be {true}
		result.get should be (gradeBoundary)
	}

	@Test
	def checkDefaultEmptySelectedNoDefault(): Unit = {
		val result = cmd.defaultGrade("1234", Map("1234" -> "60"), Map("1234" -> Seq(gradeBoundaryNoDefault)), Map("1234" -> ""))
		result.isDefined should be {false}
	}

	@Test
	def checkDefaultNullSelectedNoDefault(): Unit = {
		val result = cmd.defaultGrade("1234", Map("1234" -> "60"), Map("1234" -> Seq(gradeBoundaryNoDefault)), Map("1234" -> null))
		result.isDefined should be {false}
	}

	@Test
	def checkDefaultMissingSelectedNoDefault(): Unit = {
		val result = cmd.defaultGrade("1234", Map("1234" -> "60"), Map("1234" -> Seq(gradeBoundaryNoDefault)), Map())
		result.isDefined should be {false}
	}

	@Test
	def checkDefaultMissingSelectedZeroMark(): Unit = {
		val result = cmd.defaultGrade("1234", Map("1234" -> "0"), Map("1234" -> Seq(gradeBoundaryNoDefault)), Map())
		result.isDefined should be {false}
	}

}
