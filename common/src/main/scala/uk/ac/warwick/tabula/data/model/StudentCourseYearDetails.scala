package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}
import org.hibernate.annotations.{Any => _, _}
import org.joda.time.{DateTime, Duration}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, TermService, UserLookupService}
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.termdates.TermNotFoundException

import scala.beans.BeanProperty

object StudentCourseYearDetails {
	type YearOfStudy = Int

	final val FreshCourseYearDetailsOnlyFilter = "freshStudentCourseYearDetailsOnly"

	object Overcatting {
		final val Modules = "modules"
		final val ChosenBy = "chosenBy"
		final val ChosenDate = "chosenDate"
		final val MarkOverrides = "markOverrides"
	}
}

@FilterDefs(Array(
	new FilterDef(name = StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter, defaultCondition = "missingFromImportSince is null")
))
@Filters(Array(
	new Filter(name = StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
))
@javax.persistence.Entity
class StudentCourseYearDetails extends StudentCourseYearProperties
	with GeneratedId with ToString with HibernateVersioned with PermissionsTarget
	with Ordered[StudentCourseYearDetails] with PostLoadBehaviour {

	@transient
	var termService: TermService = Wire.auto[TermService]

	def this(studentCourseDetails: StudentCourseDetails, sceSequenceNumber: JInteger, year:AcademicYear) {
		this()
		this.studentCourseDetails = studentCourseDetails
		this.sceSequenceNumber = sceSequenceNumber
		this.academicYear = year
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	var studentCourseDetails: StudentCourseDetails = _

	def toStringProps = Seq("studentCourseDetails" -> studentCourseDetails, "sceSequenceNumber" -> sceSequenceNumber, "academicYear" -> academicYear)

	def permissionsParents: Stream[PermissionsTarget] = Stream(Option(studentCourseDetails), Option(enrolmentDepartment)).flatten

	/**
	 * This is used to calculate StudentCourseDetails.latestStudentCourseYearDetails
	 */
	def compare(that:StudentCourseYearDetails): Int = {
		if (this.studentCourseDetails.scjCode != that.studentCourseDetails.scjCode)
			this.studentCourseDetails.compare(that.studentCourseDetails)
		else if (this.academicYear != that.academicYear)
			this.academicYear.compare(that.academicYear)
		else
			this.sceSequenceNumber - that.sceSequenceNumber
	}

	def equals(that: StudentCourseYearDetails): Boolean = {
		(this.studentCourseDetails.scjCode == that.studentCourseDetails.scjCode) && (this.sceSequenceNumber == that.sceSequenceNumber)
	}

	def isFresh: Boolean = missingFromImportSince == null

	// There can be more than one StudentCourseYearDetails per year if there are multiple sequence numbers,
	// so moduleRegistrations are not attached directly - instead, get them from StudentCourseDetails,
	// filtering by year:
	def moduleRegistrations: Seq[ModuleRegistration] = studentCourseDetails.moduleRegistrations.filter(_.academicYear == this.academicYear)

	// similarly for accredited prior learning
	def accreditedPriorLearning: Seq[AccreditedPriorLearning] = {
		studentCourseDetails.accreditedPriorLearning.filter(_.academicYear == this.academicYear)
	}

	def registeredModules: Seq[Module] = moduleRegistrations.map(mr => mr.module)

	def hasModuleRegistrations: Boolean = moduleRegistrations.nonEmpty

	def hasModuleRegistrationWithNonStandardOccurrence: Boolean = moduleRegistrations.exists(_.occurrence != "A")

	def hasAccreditedPriorLearning: Boolean = accreditedPriorLearning.nonEmpty

	def isLatest: Boolean = this.equals(studentCourseDetails.latestStudentCourseYearDetails)

	def relationships(relationshipType: StudentRelationshipType): Seq[StudentRelationship] = {
		try {
			val academicYearStartDate = termService.getTermFromAcademicWeek(1, academicYear).getStartDate
			val academicYearEndDate = termService.getTermFromAcademicWeek(1, academicYear.next).getStartDate.minusDays(1)
			val twoMonthsInDays = 2 * 30

			studentCourseDetails.allRelationshipsOfType(relationshipType)
				.filter(r => r.endDate == null || r.startDate.isBefore(r.endDate))
				.filter(relationship => {
				// For the most recent YoS, only show current relationships
				if (studentCourseDetails.freshStudentCourseYearDetails.max == this) {
					relationship.isCurrent
				} else {
					// Otherwise return the relationship if it lasted for at least 6 months in this academic year
					val relationshipEndDate = if (relationship.endDate == null) academicYearEndDate else relationship.endDate
					if (relationshipEndDate.isBefore(academicYearStartDate) || relationship.startDate.isAfter(academicYearEndDate)) {
						false
					} else {
						val inYearStartDate =
							if (relationship.startDate.isBefore(academicYearStartDate))
								academicYearStartDate
							else
								relationship.startDate
						val inYearEndDate =
							if (relationshipEndDate.isAfter(academicYearEndDate))
								academicYearEndDate
							else
								relationshipEndDate
						new Duration(inYearStartDate, inYearEndDate).getStandardDays > twoMonthsInDays
					}
				}
			})
		} catch {
			case e: TermNotFoundException => Seq()
		}
	}

	def isFinalYear: Boolean = yearOfStudy.toString == studentCourseDetails.courseYearLength

	@Lob
	@Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
	protected var overcatting: Map[String, Any] = Map()

	protected def ensureOvercatting() {
		if (overcatting == null) overcatting = Map()
	}

	@transient
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

	def overcattingModules: Option[Seq[Module]] = (Option(overcatting).flatMap(_.get(StudentCourseYearDetails.Overcatting.Modules)) match {
		case Some(value: Seq[_]) => Some(value.asInstanceOf[Seq[String]])
		case _ => None
	}).map(moduleCodes => moduleCodes.flatMap(moduleAndDepartmentService.getModuleByCode))
	def overcattingModules_= (modules: Seq[Module]): Unit = overcatting += (StudentCourseYearDetails.Overcatting.Modules -> modules.map(_.code))

	@transient
	var userLookup: UserLookupService = Wire[UserLookupService]("userLookup")

	def overcattingChosenBy: Option[User] = (Option(overcatting).flatMap(_.get(StudentCourseYearDetails.Overcatting.ChosenBy)) match {
		case Some(value: String) => Some(value)
		case _ => None
	}).map(userId => userLookup.getUserByUserId(userId))
	def overcattingChosenBy_= (chosenBy: User): Unit = overcatting += (StudentCourseYearDetails.Overcatting.ChosenBy -> chosenBy.getUserId)

	def overcattingChosenDate: Option[DateTime] = Option(overcatting).flatMap(_.get(StudentCourseYearDetails.Overcatting.ChosenDate)) match {
		case Some(value: DateTime) => Some(value)
		case _ => None
	}
	def overcattingChosenDate_= (chosenDate: DateTime): Unit = overcatting += (StudentCourseYearDetails.Overcatting.ChosenDate -> chosenDate)

	final var agreedMarkUploadedDate: DateTime = _

	@Type(`type`="uk.ac.warwick.tabula.data.model.SSOUserType")
	final var agreedMarkUploadedBy: User = _

	def toExamGridEntityYear: ExamGridEntityYear = ExamGridEntityYear(
		moduleRegistrations = moduleRegistrations,
		cats = moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum,
		route = route match {
			case _: Route => route
			case _ => studentCourseDetails.currentRoute
		},
		overcattingModules = overcattingModules,
		markOverrides = None,
		studentCourseYearDetails = Some(this)
	)

	override def postLoad() {
		ensureOvercatting()
	}
}

trait BasicStudentCourseYearProperties {
	var sceSequenceNumber: JInteger = _

	/**
		* Sequence in SITS is stored as a 2-digit zero-padded number
		*/
	def sceSequenceNumberSitsFormat = f"${sceSequenceNumber.toInt}%02d"

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var yearOfStudy: JInteger = _

	@Column(name="cas_used")
	@Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
	var casUsed: JBoolean = _

	@Column(name="tier4visa")
	@Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
	var tier4Visa: JBoolean = _

	var agreedMark: JBigDecimal = _

}

trait StudentCourseYearProperties extends BasicStudentCourseYearProperties {
	var lastUpdatedDate: DateTime = DateTime.now
	var missingFromImportSince: DateTime = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleRegistrationStatusUserType")
	var moduleRegistrationStatus: ModuleRegistrationStatus = _ // cam_ssn.ssn_mrgs

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var academicYear: AcademicYear = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "routeCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var route: Route = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="enrolmentStatusCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
	var enrolmentStatus: SitsStatus = _

	// this is the department from the SCE table in SITS (Student Course Enrolment). It is likely to be the
	// same as the department on the Route table, and on the StudentCourseDetails, but in some cases, e.g. where routes
	// change ownership in different years, this might contain a different department. This indicates the
	// department responsible for administration for the student for this year.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "enrolment_department_id")
	var enrolmentDepartment: Department = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="modeOfAttendanceCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
	var modeOfAttendance: ModeOfAttendance = _

	/*
	 * A boolean flag set at import-time (see ImportStudentCourseYearCommand) that indicates whether we believe
	 * the student was enrolled in this year or not. For every year except the student's final year, this is effectively
	 * the same as checking that the enrolmentStatus is not permanently withdrawn (doesn't start with P). Because the
	 * Academic Office marks SCE records as permanently withdrawn when a student graduates, we also set this for true
	 * if the student is permanently withdrawn but their SCJ reason for transfer code starts with S (i.e. they have successfully
	 * completed their course).
	 */
	var enrolledOrCompleted: Boolean = _
}

class StudentCourseYearKey {
	@BeanProperty
	var scjCode: String = _

	@BeanProperty
	var sceSequenceNumber: JInteger = _

	def this(scjCode: String, sceSequenceNumber: JInteger) = {
		this()
		this.scjCode = scjCode
		this.sceSequenceNumber = sceSequenceNumber
	}

	override final def equals(other: scala.Any): Boolean = other match {
		case that: StudentCourseYearKey =>
			new EqualsBuilder()
				.append(scjCode, that.scjCode)
				.append(sceSequenceNumber, that.sceSequenceNumber)
				.build()
		case _ => false
	}

	override final def hashCode: Int =
		new HashCodeBuilder()
				.append(scjCode)
				.append(sceSequenceNumber)
			.build()
}

