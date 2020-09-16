package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.mitcircs.RenderMitCircsAttachmentCommand
import uk.ac.warwick.tabula.commands.mitcircs.submission._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.mitcircs.{CoronavirusIssueType, IssueType, MitCircsContact, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.{CourseType, Member, Module, StudentMember}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.mitcircs.MitCircsSubmissionService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs
import uk.ac.warwick.tabula.web.controllers.profiles.profile.AbstractViewProfileController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, FeaturesComponent, ItemNotFoundException}
import scala.jdk.CollectionConverters._
import scala.collection.immutable.ListMap

abstract class AbstractMitCircsFormController extends AbstractViewProfileController with FeaturesComponent {

  validatesSelf[SelfValidating]

  var mitCircsSubmissionService: MitCircsSubmissionService = Wire[MitCircsSubmissionService]

  @ModelAttribute("registeredYears")
  def registeredYears(@PathVariable student: StudentMember): Seq[AcademicYear] =
    student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails).map(_.academicYear).distinct.sorted.reverse

  @ModelAttribute("registeredModules")
  def registeredModules(@PathVariable student: StudentMember): ListMap[AcademicYear, Seq[Module]] = {
    val builder = ListMap.newBuilder[AcademicYear, Seq[Module]]

    student.moduleRegistrationsByYear(None)
      .filter { mr => mr.agreedMark.isEmpty && mr.agreedGrade.isEmpty }
      .groupBy(_.academicYear)
      .view.mapValues(_.map(_.module).toSeq.sorted)
      .toSeq
      .sortBy { case (year, _) => -year.startYear }
      .foreach { case (year, modules) => builder += year -> modules }

    builder.result()
  }

  @ModelAttribute("includeEngagementCriteria")
  def includeEngagementCriteria(@PathVariable student: StudentMember): Boolean =
    Option(student.mostSignificantCourse).flatMap(scd => Option(scd.award)).map(_.code).contains("MBCHB")

  @ModelAttribute("previousSubmissions")
  def previousSubmissions(
    @PathVariable student: StudentMember,
    @PathVariable(required=false) submission: MitigatingCircumstancesSubmission
  ): Set[MitigatingCircumstancesSubmission] = {
    val allSubmissions = mitCircsSubmissionService.submissionsForStudent(student)
    allSubmissions.toSet -- Option(submission).toSet
  }

  @ModelAttribute("personalTutors")
  def personalTutors(@PathVariable student: StudentMember): Set[Member] = {
    relationshipService.getStudentRelationshipTypeByUrlPart("tutor")
      .map(tutor => relationshipService.findCurrentRelationships(tutor, student))
      .getOrElse(Nil)
      .flatMap(_.agentMember)
      .toSet
  }

  @RequestMapping
  def form(@ModelAttribute("student") student: StudentMember): Mav = {
    Mav("mitcircs/submissions/form", Map(
      "issueTypes" -> IssueType.validIssueTypes(student),
      "covid19IssueTypes" -> IssueType.coronavirusIssueTypes,
      "possibleContacts" -> MitCircsContact.values,
      "department" -> Option(student.homeDepartment).flatMap(_.subDepartmentsContaining(student).find(_.enableMitCircs)),
    )).crumbs(breadcrumbsStudent(activeAcademicYear, student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)
  }

}

@Controller
@RequestMapping(value = Array("/profiles/view/{student}/personalcircs/mitcircs/new"))
class CreateMitCircsController extends AbstractMitCircsFormController {

  type CreateCommand = Appliable[MitigatingCircumstancesSubmission] with MitCircsSubmissionState with SelfValidating

  @ModelAttribute("command") def create(@PathVariable student: StudentMember, user: CurrentUser): CreateCommand = {
    if (student.mostSignificantCourse.courseType.contains(CourseType.PGR)) {
      throw new ItemNotFoundException(student, "PGRs cannot raise mitigating circumstances submissions")
    }

    CreateMitCircsSubmissionCommand(mandatory(student), user.apparentUser)
  }

  @ModelAttribute("student") def student(@PathVariable student: StudentMember): StudentMember = student

  @RequestMapping(method = Array(POST))
  def save(@Valid @ModelAttribute("command") cmd: CreateCommand, errors: Errors, @PathVariable student: StudentMember): Mav = {
    if (errors.hasErrors) form(student).addObjects("errors" -> errors)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Profile.PersonalCircumstances.view(submission))
    }
  }
}

@Controller
@RequestMapping(value = Array("/profiles/view/{student}/personalcircs/mitcircs/edit/{submission}"))
class EditMitCircsController extends AbstractMitCircsFormController {

  type EditCommand = Appliable[MitigatingCircumstancesSubmission] with EditMitCircsSubmissionState with SelfValidating

  @ModelAttribute("command") def edit(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable student: StudentMember,
    user: CurrentUser
  ): EditCommand = {
    mustBeLinked(submission, student)
    if (!submission.isEditable(user.apparentUser)) throw new ItemNotFoundException(submission, "Not displaying mitigating circumstances submission as it is not currently editable")
    EditMitCircsSubmissionCommand(submission, user.apparentUser)
  }

  @ModelAttribute("student") def student(@PathVariable submission: MitigatingCircumstancesSubmission): StudentMember =
    submission.student

  @ModelAttribute("lastUpdatedByOther") def lastUpdatedByOther(@PathVariable submission: MitigatingCircumstancesSubmission, user: CurrentUser): Boolean = {
    val studentUser = submission.student.asSsoUser
    user.apparentUser == studentUser && submission.lastModifiedBy != studentUser
  }

  @RequestMapping(method = Array(POST))
  def save(@Valid @ModelAttribute("command") cmd: EditCommand, errors: Errors, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    if (errors.hasErrors) {
      logger.warn(s"Validation failed for mit circs submission ${submission.key} by ${submission.student.id}. ${errors.getAllErrors.asScala.map(_.toString)}")
      form(submission.student).addObjects("errors" -> errors)
    } else {
      val submission = cmd.apply()
      RedirectForce(Routes.Profile.PersonalCircumstances.view(submission))
    }
  }

}

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}/supporting-file/{filename}"))
class MitCircsAttachmentController extends BaseController {

  type RenderAttachmentCommand = Appliable[Option[RenderableAttachment]]

  @ModelAttribute("renderAttachmentCommand")
  def attachmentCommand(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable filename: String
  ): RenderAttachmentCommand =
    RenderMitCircsAttachmentCommand(mandatory(submission), mandatory(filename))

  @RequestMapping(method = Array(GET))
  def supportingFile(@ModelAttribute("renderAttachmentCommand") attachmentCommand: RenderAttachmentCommand, @PathVariable filename: String): RenderableFile =
    attachmentCommand.apply().getOrElse(throw new ItemNotFoundException())

}

@Controller
@RequestMapping(Array("/profiles/view/{student}/personalcircs/mitcircs/edit/{submission}/withdraw"))
class WithdrawMitCircsSubmissionController extends AbstractViewProfileController {

  @ModelAttribute("command")
  def command(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable student: StudentMember,
    user: CurrentUser
  ): WithdrawMitCircsSubmissionCommand.Command = {
    mustBeLinked(submission, student)
    if (!user.universityId.maybeText.contains(student.universityId)) throw new ItemNotFoundException(submission, "Not displaying mitigating circumstances submission as can only be withdrawn by self")
    WithdrawMitCircsSubmissionCommand(submission, user.apparentUser)
  }

  @RequestMapping
  def form(@PathVariable student: StudentMember): Mav =
    Mav("mitcircs/submissions/withdraw")
      .crumbs(breadcrumbsStudent(activeAcademicYear, student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)

  @PostMapping
  def withdraw(@ModelAttribute("command") command: WithdrawMitCircsSubmissionCommand.Command, errors: Errors, @PathVariable submission: MitigatingCircumstancesSubmission): Mav =
    if (errors.hasErrors) form(command.student)
    else RedirectForce(Routes.Profile.PersonalCircumstances.view(command.apply()))

}

@Controller
@RequestMapping(Array("/profiles/view/{student}/personalcircs/mitcircs/edit/{submission}/reopen"))
class ReopenMitCircsSubmissionController extends AbstractViewProfileController {

  @ModelAttribute("command")
  def command(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable student: StudentMember,
    user: CurrentUser
  ): ReopenMitCircsSubmissionCommand.Command = {
    mustBeLinked(submission, student)
    if (!user.universityId.maybeText.contains(student.universityId)) throw new ItemNotFoundException(submission, "Not displaying mitigating circumstances submission as can only be reopened by self")
    ReopenMitCircsSubmissionCommand(submission, user.apparentUser)
  }

  @RequestMapping
  def form(@PathVariable student: StudentMember): Mav =
    Mav("mitcircs/submissions/reopen")
      .crumbs(breadcrumbsStudent(activeAcademicYear, student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)

  @PostMapping
  def reopen(@ModelAttribute("command") command: WithdrawMitCircsSubmissionCommand.Command, errors: Errors, @PathVariable submission: MitigatingCircumstancesSubmission): Mav =
    if (errors.hasErrors) form(command.student)
    else RedirectForce(Routes.Profile.PersonalCircumstances.edit(command.apply()))

}
