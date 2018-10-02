package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.profiles.profile.ViewProfileCommand
import uk.ac.warwick.tabula.commands.profiles.{SearchProfilesCommand, SearchProfilesCommandInternal, SearchProfilesCommandPermissions}
import uk.ac.warwick.tabula.commands.{ComposableCommand, MemberOrUser, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.services.{AutowiringAssessmentServiceComponent, AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs.Profile.ProfileBreadcrumbIdentifier
import uk.ac.warwick.tabula.web.controllers.profiles.{ProfileBreadcrumbs, ProfilesController}
import uk.ac.warwick.tabula.web.{BreadCrumb, Breadcrumbs => BaseBreadcumbs}

import scala.collection.JavaConverters._

abstract class AbstractViewProfileController extends ProfilesController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent
	with AutowiringAssessmentServiceComponent
	with RequestLevelCaching[String, Any] {

	@ModelAttribute("siblingBreadcrumbs")
	def siblingBreadcrumbs = true

	// Only do this once per request
	protected def relationshipTypesToDisplay(scd: StudentCourseDetails): Seq[StudentRelationshipType] =
		cachedBy(s"relationshipTypesToDisplay-${scd.id}") {
			relationshipService.allStudentRelationshipTypes.filter(relationshipType =>
				scd.hasRelationship(relationshipType) ||
					relationshipType.displayIfEmpty(scd) &&	scd.isStudentRelationshipTypeForDisplay(relationshipType)
			)
		} match {
			case relationshipTypes: Seq[StudentRelationshipType]@unchecked => relationshipTypes
			case _ => throw new UnsupportedOperationException("Not a Seq[StudentRelationshipType]")
		}

	// Only do this once per request
	protected def scydToSelect(scd: StudentCourseDetails, activeAcademicYear: Option[AcademicYear]): Option[StudentCourseYearDetails] =
		cachedBy(s"scydToSelect-${scd.id}") {
			val scyds = scd.freshStudentCourseYearDetails match {
				case Nil =>
					scd.freshOrStaleStudentCourseYearDetails
				case fresh =>
					fresh
			}
			val thisAcademicYear = activeAcademicYear.getOrElse(AcademicYear.now())
			Option(scyds.find(_.academicYear == thisAcademicYear).getOrElse {
				if (thisAcademicYear > scyds.last.academicYear)
					scyds.last
				else
					scyds.head
			})
		} match {
			case scyd: Option[StudentCourseYearDetails]@unchecked => scyd
			case _ => throw new UnsupportedOperationException("Not a Option[StudentCourseYearDetails]")
		}

	protected def breadcrumbsStudent(
		activeAcademicYear: Option[AcademicYear],
		scd: StudentCourseDetails,
		activeIdentifier: ProfileBreadcrumbIdentifier
	): Seq[BreadCrumb] = {
		scydToSelect(scd, activeAcademicYear) match {
			case None =>
				breadcrumbsStaff(scd.student, activeIdentifier)
			case Some(scyd) =>
				Seq(
					ProfileBreadcrumbs.Profile.IdentityForScyd(scyd).setActive(activeIdentifier),
					ProfileBreadcrumbs.Profile.TimetableForScyd(scyd).setActive(activeIdentifier)
				) ++ relationshipTypesToDisplay(scd).map(relationshipType =>
					ProfileBreadcrumbs.Profile.RelationshipTypeForScyd(scyd, relationshipType).setActive(activeIdentifier)
				) ++ Seq(
					ProfileBreadcrumbs.Profile.AssignmentsForScyd(scyd).setActive(activeIdentifier),
					ProfileBreadcrumbs.Profile.ModulesForScyd(scyd).setActive(activeIdentifier),
					ProfileBreadcrumbs.Profile.EventsForScyd(scyd).setActive(activeIdentifier),
					ProfileBreadcrumbs.Profile.AttendanceForScyd(scyd).setActive(activeIdentifier)
				) ++ (assessmentService.getAssignmentWhereMarker(MemberOrUser(scd.student).asUser, Some(scyd.academicYear)) match {
					case Nil => Nil
					case _ => Seq(ProfileBreadcrumbs.Profile.MarkingForScyd(scyd).setActive(activeIdentifier))
				}) ++ (scd.student == currentMember match {
					case false => Nil
					case true => Seq(ProfileBreadcrumbs.Profile.DownloadForScyd(scyd).setActive(activeIdentifier))
				})
		}

	}

	protected def breadcrumbsStaff(member: Member, activeIdentifier: ProfileBreadcrumbIdentifier): Seq[BreadCrumb] = Seq(
		ProfileBreadcrumbs.Profile.Identity(member).setActive(activeIdentifier),
		ProfileBreadcrumbs.Profile.Timetable(member).setActive(activeIdentifier),
		ProfileBreadcrumbs.Profile.Marking(member).setActive(activeIdentifier),
		ProfileBreadcrumbs.Profile.Students(member).setActive(activeIdentifier)
	)

	protected def secondBreadcrumbs(activeAcademicYear: Option[AcademicYear], scd: StudentCourseDetails)(urlGenerator: (StudentCourseYearDetails) => String): Seq[BreadCrumb] = {
		scydToSelect(scd, activeAcademicYear).map(chooseScyd => {
			val scyds = scd.student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails) match {
				case Nil =>
					scd.student.freshOrStaleStudentCourseDetails.flatMap(_.freshOrStaleStudentCourseYearDetails)
				case fresh =>
					fresh
			}
			scyds.map(scyd =>
				BaseBreadcumbs.Standard(
					title = "%s %s".format(scyd.studentCourseDetails.course.code, scyd.academicYear.getLabel),
					url = Some(urlGenerator(scyd)),
					tooltip = "%s %s".format(
						scyd.studentCourseDetails.course.name,
						scyd.academicYear.getLabel
					)
				).setActive(scyd == chooseScyd)
			)
		}).getOrElse(Nil).toSeq
	}

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable(required = false) academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@Autowired var conversionService: ConversionService = _

	private def convertOrNull[A >: Null](source: Object, targetType: Class[A]): A = {
		try {
			conversionService.convert(source, targetType)
		} catch {
			case _: Exception => null
		}
	}

	@ModelAttribute("viewProfileCommand")
	protected def viewProfileCommand(@PathVariable pvs: JMap[String, String]): Any = {
		val pathVariables = pvs.asScala
		if (pathVariables.contains("member")) {
			new ViewProfileCommand(user, mandatory(
				convertOrNull(pathVariables("member"), classOf[Member])
			))
		} else if (pathVariables.contains("studentCourseDetails")) {
			new ViewProfileCommand(user, mandatory(
				convertOrNull(pathVariables("studentCourseDetails"), classOf[StudentCourseDetails])
			).student)
		}
	}


	@ModelAttribute("searchProfilesCommand")
	protected def searchProfilesCommand: SearchProfilesCommandInternal with ComposableCommand[Seq[Member]] with Unaudited with SearchProfilesCommandPermissions =
		restricted(SearchProfilesCommand(currentMember, user)).orNull

}