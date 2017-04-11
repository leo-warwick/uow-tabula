package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.{ToString, AcademicYear}
import uk.ac.warwick.tabula.system.permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports._
import org.apache.commons.lang3.builder.CompareToBuilder

/*
 * scj code, award code and sequence number together make up the key in SITS, and form a unique index in Tabula.
 */

@Entity
@Access(AccessType.FIELD)
class AccreditedPriorLearning() extends GeneratedId	with PermissionsTarget with ToString with Ordered[AccreditedPriorLearning] {
	def this(studentCourseDetails: StudentCourseDetails,
		award: Award,
		sequenceNumber: Int,
		academicYear: AcademicYear,
		cats: JBigDecimal,
		level: Level,
		reason: String) {
		this()
		this.studentCourseDetails = studentCourseDetails
		this.award = award
		this.sequenceNumber = sequenceNumber
		this.academicYear = academicYear
		this.cats = cats
		this.level = level
		this.reason = reason
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	@Restricted(Array("Profiles.Read.AccreditedPriorLearning"))
	var studentCourseDetails: StudentCourseDetails = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="awardCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.AccreditedPriorLearning"))
	var award: Award = null

	var sequenceNumber: JInteger = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Restricted(Array("Profiles.Read.AccreditedPriorLearning"))
	var academicYear: AcademicYear = null

	@Restricted(Array("Profiles.Read.AccreditedPriorLearning"))
	var cats: JBigDecimal = null

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="levelCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.AccreditedPriorLearning"))
	var level: Level = _

	@Restricted(Array("Profiles.Read.AccreditedPriorLearning"))
	var reason: String = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var lastUpdatedDate: DateTime = DateTime.now

	def toStringProps = Seq(
		"scjCode" -> studentCourseDetails.scjCode,
		"awardCode" -> award.code,
		"sequenceNumber" -> sequenceNumber)

	def permissionsParents: Stream[StudentCourseDetails] = Stream(Option(studentCourseDetails)).flatten

	override def compare(that: AccreditedPriorLearning): Int = {
		new CompareToBuilder()
		.append(studentCourseDetails, that.studentCourseDetails)
		.append(academicYear, that.academicYear)
		.append(award, that.award)
		.append(cats, that.cats)
		.build()
	}

}
