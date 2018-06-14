package uk.ac.warwick.tabula.jobs.exams

import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.support.FormattingConversionService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.validation.{BindingResult, DataBinder}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridCheckAndApplyOvercatCommand, GenerateExamGridGridOptionsCommand, GenerateExamGridSelectCourseCommand}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.exams.grids.JobInstanceStatusAdapter
import uk.ac.warwick.tabula.exams.grids.documents.ExamGridDocument._
import uk.ac.warwick.tabula.exams.grids.documents.{ExamGridDocument, ExamGridDocumentPrototype}
import uk.ac.warwick.tabula.jobs.{Job, JobPrototype}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringModuleRegistrationServiceComponent, AutowiringSecurityServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object GenerateExamGridDocumentJob {
	val identifier = "exam-document"

	def apply(
		document: ExamGridDocumentPrototype,
		department: Department,
		academicYear: AcademicYear,
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		options: Map[String, Any] = Map.empty
	): JobPrototype = JobPrototype(identifier, Map(
		"documentIdentifier" -> document.identifier,
		"options" -> options,
		"department" -> department.code,
		"academicYear" -> academicYear.getStoreValue,
		"selectCourseCommand" -> selectCourseCommand.toMap,
		"gridOptionsCommand" -> gridOptionsCommand.toMap
	))
}

@Component
class GenerateExamGridDocumentJob extends Job with AutowiringSecurityServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringModuleRegistrationServiceComponent with AutowiringFileAttachmentServiceComponent with TaskBenchmarking {
	override val identifier: String = GenerateExamGridDocumentJob.identifier

	@Autowired var documents: Array[ExamGridDocument] = Array()

	val departmentPermission: Permission = Permissions.Department.ExamGrids

	def selectCourseCommand(user: CurrentUser, department: Department, academicYear: AcademicYear) =
		GenerateExamGridSelectCourseCommand(department, academicYear, permitRoutesFromRootDepartment = securityService.can(user, departmentPermission, department.rootDepartment))

	def gridOptionsCommand(department: Department) = GenerateExamGridGridOptionsCommand(department)

	def checkOvercatCommand(user: CurrentUser, department: Department, academicYear: AcademicYear) =
		GenerateExamGridCheckAndApplyOvercatCommand(department, academicYear, user)

	def bind(obj: Any, propsKey: String)(implicit job: JobInstance): BindingResult = {
		val pvs = new MutablePropertyValues()
		job.propsMap(propsKey).asInstanceOf[Map[String, Any]].foreach {
			case (name, value: Iterable[Any]) => pvs.add(name, value.asJava)
			case (name, value) => pvs.add(name, value)
		}

		val binder = new DataBinder(obj)
		binder.setConversionService(Wire[FormattingConversionService])
		binder.bind(pvs)
		binder.getBindingResult
	}

	override def run(implicit job: JobInstance): Unit = transactional(propagation = Propagation.SUPPORTS) {
		updateStatus("Preparing")

		val department = moduleAndDepartmentService.getDepartmentByCode(job.getString("department")).get
		val academicYear = AcademicYear.apply(job.getString("academicYear").toInt)

		val selectCourseCommandInstance = selectCourseCommand(job.user, department, academicYear)
		val gridOptionsCommandInstance = gridOptionsCommand(department)

		val selectCourseCommandErrors = bind(selectCourseCommandInstance, "selectCourseCommand")
		val gridOptionsCommandErrors = bind(gridOptionsCommandInstance, "gridOptionsCommand")

		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}

		val checkOvercatCommandInstance = checkOvercatCommand(job.user, department, academicYear)

		val document = documents.find(_.identifier == job.getString("documentIdentifier")).getOrElse(throw new IllegalArgumentException("Invalid document identifier"))

		val statusAdapter = new JobInstanceStatusAdapter(this, job)
		val options: Map[String, Any] = job.propsMap.get("options").fold(Map.empty[String, Any])(_.asInstanceOf[Map[String, Any]])

		val file = benchmarkTask(document.getClass.getSimpleName) {
			document.apply(
				department = department,
				academicYear = academicYear,
				selectCourseCommand = selectCourseCommandInstance,
				gridOptionsCommand = gridOptionsCommandInstance,
				checkOvercatCommand = checkOvercatCommandInstance,
				options = options,
				status = statusAdapter
			)
		}

		updateStatus("Saving the document")

		fileAttachmentService.saveTemporary(file)

		job.setString("fileId", file.id)

		updateStatus("Document ready")

		updateProgress(100)
		job.succeeded = true
	}
}
