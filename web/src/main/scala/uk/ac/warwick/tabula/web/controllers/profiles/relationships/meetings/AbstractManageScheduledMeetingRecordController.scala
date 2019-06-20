package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import javax.validation.Valid
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

abstract class AbstractManageScheduledMeetingRecordController extends ProfilesController {

  validatesSelf[SelfValidating]

  @ModelAttribute("allRelationships")
  def allRelationships(
    @PathVariable studentCourseDetails: StudentCourseDetails
  ): Seq[StudentRelationship] = {
    relationshipService.findCurrentRelationships(mandatory(studentCourseDetails))
  }

  @ModelAttribute("schedulableRelationships")
  def schedulableRelationships(
    @PathVariable studentCourseDetails: StudentCourseDetails
  ): Seq[StudentRelationship] = {
    val isSelf = user.universityId.maybeText.contains(studentCourseDetails.student.universityId)

    relationshipService.findCurrentRelationships(mandatory(studentCourseDetails))
      .filter(relationship => !isSelf || relationship.agentMember.exists(_.homeDepartment.studentsCanScheduleMeetings))
  }

  @ModelAttribute("nonSchedulableRelationships")
  def nonSchedulableRelationships(
    @PathVariable studentCourseDetails: StudentCourseDetails
  ): Seq[StudentRelationship] = {
    val isSelf = user.universityId.maybeText.contains(studentCourseDetails.student.universityId)

    relationshipService.findCurrentRelationships(mandatory(studentCourseDetails))
      .filterNot(relationship => !isSelf || relationship.agentMember.exists(_.homeDepartment.studentsCanScheduleMeetings))
  }

  @ModelAttribute("manageableRelationships")
  def manageableRelationships(
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @ModelAttribute("allRelationships") allRelationships: Seq[StudentRelationship]
  ): Seq[StudentRelationship] = {
    allRelationships.filter(r => securityService.can(user, Permissions.Profiles.ScheduledMeetingRecord.Manage(r.relationshipType), studentCourseDetails.student))
  }

  @ModelAttribute("manageableSchedulableRelationships")
  def manageableSchedulableRelationships(
    @ModelAttribute("manageableRelationships") manageableRelationships: Seq[StudentRelationship],
    @ModelAttribute("schedulableRelationships") schedulableRelationships: Seq[StudentRelationship]
  ): Seq[StudentRelationship] = schedulableRelationships.filter(manageableRelationships.contains)

  @ModelAttribute("nonManageableRelationships")
  def nonManageableRelationships(
    @ModelAttribute("allRelationships") allRelationships: Seq[StudentRelationship],
    @ModelAttribute("nonSchedulableRelationships") nonSchedulableRelationships: Seq[StudentRelationship],
    @ModelAttribute("manageableRelationships") manageableRelationships: Seq[StudentRelationship]
  ): Seq[StudentRelationship] = {
    allRelationships.filterNot(nonSchedulableRelationships.contains).filterNot(manageableRelationships.contains)
  }

  @RequestMapping(method = Array(GET, HEAD), params = Array("iframe"))
  def getIframe(
    @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord] with PopulateOnForm,
    @PathVariable relationshipType: StudentRelationshipType,
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    cmd.populate()
    form(cmd, relationshipType, studentCourseDetails, academicYear, iframe = true)
  }

  @RequestMapping(method = Array(GET, HEAD))
  def get(
    @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord] with PopulateOnForm,
    @PathVariable relationshipType: StudentRelationshipType,
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    cmd.populate()
    form(cmd, relationshipType, studentCourseDetails, academicYear)
  }

  private def form(
    cmd: Appliable[ScheduledMeetingRecord],
    relationshipType: StudentRelationshipType,
    studentCourseDetails: StudentCourseDetails,
    academicYear: AcademicYear,
    iframe: Boolean = false
  ) = {
    val mav = Mav("profiles/related_students/meeting/schedule",
      "returnTo" -> getReturnTo(Routes.Profile.relationshipType(studentCourseDetails, academicYear, relationshipType)),
      "isModal" -> ajax,
      "isIframe" -> iframe,
      "formats" -> MeetingFormat.members
    )
    if (ajax)
      mav.noLayout()
    else if (iframe)
      mav.noNavigation()
    else
      mav
  }

  @RequestMapping(method = Array(POST), params = Array("iframe"))
  def submitIframe(
    @Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
    errors: Errors,
    @PathVariable relationshipType: StudentRelationshipType,
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    if (errors.hasErrors) {
      form(cmd, relationshipType, studentCourseDetails, academicYear, iframe = true)
    } else {
      cmd.apply()
      Mav("profiles/related_students/meeting/schedule",
        "success" -> true
      )
    }
  }

  @RequestMapping(method = Array(POST))
  def submit(
    @Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
    errors: Errors,
    @PathVariable relationshipType: StudentRelationshipType,
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    if (errors.hasErrors) {
      form(cmd, relationshipType, studentCourseDetails, academicYear)
    } else {
      cmd.apply()
      Redirect(Routes.Profile.relationshipType(studentCourseDetails, academicYear, relationshipType))
    }
  }

}
