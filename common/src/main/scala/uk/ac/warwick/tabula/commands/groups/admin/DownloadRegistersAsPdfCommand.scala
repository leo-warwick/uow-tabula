package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.{DateTime, Interval}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.groups.admin.DownloadRegistersAsPdfHelper._
import uk.ac.warwick.tabula.commands.{MemberOrUser, _}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventOccurrence, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Department, UserSettings}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.ComposableOrdering

import scala.collection.JavaConverters._

object DownloadRegistersAsPdfCommand {

	def apply(department: Department, academicYear: AcademicYear, filename: String, user: CurrentUser) =
		new DownloadRegistersAsPdfCommandInternal(department, academicYear, filename, user)
			with ComposableCommandWithoutTransaction[RenderableFile]
			with AutowiringDownloadRegistersAsPdfCommandHelper
			with DownloadRegistersAsPdfValidation
			with DownloadRegistersAsPdfPermissions
			with DownloadRegistersAsPdfCommandState
			with DownloadRegistersAsPdfCommandRequest
			with GetsOccurrencesForDownloadRegistersAsPdfCommand
			with Unaudited
}

class DownloadRegistersAsPdfCommandInternal(val department: Department, val academicYear: AcademicYear, filename: String, user: CurrentUser)
	extends CommandInternal[RenderableFile] with TaskBenchmarking {

	self: DownloadRegistersAsPdfHelper.Dependencies =>

	override def applyInternal(): RenderableAttachment = {
		val userSettings = transactional(readOnly = true) { userSettingsService.getByUserId(user.apparentId).getOrElse(new UserSettings(user.apparentId)) }
		userSettings.registerPdfShowPhotos = showPhotos
		userSettings.registerPdfDisplayName = displayName
		userSettings.registerPdfDisplayCheck = displayCheck
		userSettings.registerPdfSortOrder = sortOrder
		transactional() { userSettingsService.save(user, userSettings) }

		val sortedOccurrences = sortOrder match {
			case SortOrder.Module => getOccurrences.sortBy(o => (
				o.event.group.groupSet.module,
				o.dateTime(weeksForYear).get
			))
			case _ => getOccurrences.sortBy(o => (
				// Where there is more than one tutor, sort them by name and pick the first one, then sort all the occurrences by first tutor name
				// If there are no tutors but the occurrence last
				o.event.tutors.users
					.sortBy(u => (u.getLastName, u.getFirstName))
					.headOption
					.map(u => (u.getLastName, u.getFirstName))
					.getOrElse(("ZZZZ", "ZZZZ")),
				o.dateTime(weeksForYear).get
			))
		}

		// Get all the users and memebrs up front so we only have to call the profile service once
		val regularUserMap: Map[String, User] = sortedOccurrences.flatMap(o => o.event.group.students.users).groupBy(_.getWarwickId).mapValues(_.head)
		val extraAttendanceUserMap: Map[String, User] = userLookup.getUsersByWarwickUniIds(sortedOccurrences.flatMap(_.attendance.asScala.map(_.universityId)).distinct)
		val userMap = regularUserMap ++ extraAttendanceUserMap
		val allMembers = transactional(readOnly = true) { profileService.getAllMembersWithUniversityIds(userMap.keys.toSeq) }
		val memberOrUserMap: Map[String, MemberOrUser] = userMap.mapValues(u => MemberOrUser(allMembers.find(_.universityId == u.getWarwickId), u))

		def toOrdering(order: String): Ordering[String] = order match {
			case "desc" => Ordering.String.reverse
			case _ => Ordering.String
		}
		def toFieldOrdering(fieldName: String, index: Int): Ordering[MemberOrUser] = fieldName match {
			case "firstName" => Ordering.by[MemberOrUser, String](_.firstName)(toOrdering(studentSortOrders.get(index)))
			case "lastName" => Ordering.by[MemberOrUser, String](_.lastName)(toOrdering(studentSortOrders.get(index)))
			case _ => Ordering.by[MemberOrUser, String](_.universityId)(toOrdering(studentSortOrders.get(index)))
		}

		val fileAttachments = sortedOccurrences.map(occurrence => {

			val memberOrdering = studentSortFields.asScala match {
				case fields if fields.isEmpty =>
					Ordering.by[MemberOrUser, (String, String, String)](mou => (mou.lastName, mou.firstName, mou.universityId))
				case nonEmptySortFields =>
					new ComposableOrdering(nonEmptySortFields.zipWithIndex.map { case (fieldName, index) => toFieldOrdering(fieldName, index)}: _*)
			}

			val members: Seq[MemberOrUser] = (
				occurrence.event.group.students.users.map(u => memberOrUserMap.getOrElse(u.getWarwickId, MemberOrUser(None, new AnonymousUser))) ++
				occurrence.attendance.asScala.toSeq.map(a => memberOrUserMap.getOrElse(a.universityId, MemberOrUser(None, new AnonymousUser)))
			).distinct.sorted(memberOrdering)

			benchmarkTask("renderTemplateAndStore") {
				pdfGenerator.renderTemplateAndStore(
					DownloadRegistersAsPdfHelper.registerTemplate,
					s"register-${occurrence.id}.pdf",
					Map(
						"event" -> occurrence.event,
						"week" -> occurrence.week,
						"formattedEventDate" -> occurrence.dateTime.get.toString("dd/MM/yyyy"),
						"members" -> members,
						"showPhotos" -> showPhotos,
						"displayName" -> displayName,
						"displayCheck" -> displayCheck
					)
				)
			}

		})

		new RenderableAttachment(combinePdfs(fileAttachments, filename))
	}

}

trait DownloadRegistersAsPdfValidation extends SelfValidating {

	self: DownloadRegistersAsPdfCommandState with DownloadRegistersAsPdfCommandRequest with GetsOccurrences =>

	override def validate(errors: Errors) {
		if (startDate ==  null) {
			errors.rejectValue("startDate", "NotEmpty")
		}
		if (endDate ==  null) {
			errors.rejectValue("endDate", "NotEmpty")
		}
		if (smallGroupSets.isEmpty) {
			errors.rejectValue("smallGroupSets", "NotEmpty")
		}

		val invalidSmallGroupSets = smallGroupSets.asScala.filterNot(smallGroupsInDepartment.contains)
		if (invalidSmallGroupSets.nonEmpty) {
			errors.rejectValue("smallGroupSets", "smallGroupSet.invalidForDepartment")
		}

		if (getOccurrences.isEmpty) {
			errors.reject("smallGroups.noEvents")
		}
	}

}

trait DownloadRegistersAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DownloadRegistersAsPdfCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, department)
	}

}

trait DownloadRegistersAsPdfCommandState {

	self: TermServiceComponent with SmallGroupServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	lazy val weeksForYear: Map[Integer, Interval] = termService.getAcademicWeeksForYear(academicYear.dateInTermOne).toMap
	lazy val smallGroupsInDepartment: Seq[SmallGroupSet] = smallGroupService.getSmallGroupSets(department, academicYear).sortBy(sgs => (sgs.module, sgs.name))
}

trait GetsOccurrencesForDownloadRegistersAsPdfCommand extends GetsOccurrences with TaskBenchmarking {

	self: DownloadRegistersAsPdfCommandRequest with DownloadRegistersAsPdfCommandState
		with SmallGroupServiceComponent with TermServiceComponent =>

	override lazy val getOccurrences: Seq[SmallGroupEventOccurrence] = benchmarkTask("getOccurrences") {
		def toWeekNumber(date: DateTime) = termService.getTermFromDateIncludingVacations(date).getAcademicWeekNumber(date)

		// If the start/end date is outside the specified academic year, set to the min/max for initial filtering
		val startWeek = {
			if (startDate.isBefore(termService.getTermFromDate(academicYear.dateInTermOne).getStartDate)) {
				0
			} else {
				toWeekNumber(startDate)
			}
		}
		val endWeek = {
			if (endDate.isAfter(termService.getTermFromDate(academicYear.next.dateInTermOne).getStartDate)) {
				60
			} else {
				toWeekNumber(endDate)
			}
		}
		val events: Seq[SmallGroupEvent] = transactional(readOnly = true) { smallGroupSets.asScala.flatMap(_.groups.asScala.flatMap(_.events)) }
		// Get the occurrences that happen on the week of the start or end date, or a week in-between,
		// so we don't have to calculate the true date time of every event
		val roughOccurrances: Seq[SmallGroupEventOccurrence] = events.flatMap(event => {
			event.allWeeks.filter(w => w >= startWeek && w <= endWeek).flatMap(w =>
				transactional(readOnly = true) { smallGroupService.getOrCreateSmallGroupEventOccurrence(event, w) }
			)
		})
		// Now filter each occurrence to see if it's really between the dates
		roughOccurrances.filter(o => o.dateTime.isDefined).filter(o => {
			val dateTime = o.dateTime(weeksForYear)
			dateTime.isDefined &&
				(dateTime.get.isAfter(startDate) || dateTime.get.isEqual(startDate)) &&
					(dateTime.get.isBefore(endDate) || dateTime.get.isEqual(endDate))
		})
	}
}
