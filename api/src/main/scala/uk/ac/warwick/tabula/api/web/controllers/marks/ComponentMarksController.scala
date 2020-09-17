package uk.ac.warwick.tabula.api.web.controllers.marks

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.RecordAssessmentComponentMarksCommand.StudentMarksItem
import uk.ac.warwick.tabula.commands.marks.{ListAssessmentComponentsCommand, RecordAssessmentComponentMarksCommand}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Module, UpstreamAssessmentGroup, UpstreamAssessmentGroupInfo}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.AutowiringAssessmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.marks.{AutowiringAssessmentComponentMarksServiceComponent, AutowiringResitServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(value = Array("/v1/marks/module/{module}-{cats}/{academicYear}/{occurrence}/component/{assessmentGroup}/{sequence}"), produces = Array("application/json"))
class ComponentMarksController extends ApiController
  with AutowiringAssessmentMembershipServiceComponent
  with AutowiringAssessmentComponentMarksServiceComponent
  with AutowiringResitServiceComponent {

  @ModelAttribute("assessmentComponent")
  def assessmentComponent(
    // Identify the module
    @PathVariable module: Module,
    @PathVariable cats: BigDecimal,

    // Identify the assessment component
    @PathVariable assessmentGroup: String,
    @PathVariable sequence: String,
  ): AssessmentComponent = mandatory {
    assessmentMembershipService.getAssessmentComponents(mandatory(module), inUseOnly = false)
      .find { ac =>
        ac.cats.map(BigDecimal(_).setScale(1, BigDecimal.RoundingMode.HALF_UP)).contains(mandatory(cats).setScale(1, BigDecimal.RoundingMode.HALF_UP)) &&
        ac.assessmentGroup == assessmentGroup &&
        ac.sequence == sequence
      }
  }

  @ModelAttribute("upstreamAssessmentGroup")
  def upstreamAssessmentGroup(
    @ModelAttribute("assessmentComponent") assessmentComponent: AssessmentComponent,
    @PathVariable academicYear: AcademicYear,
    @PathVariable occurrence: String,
  ): UpstreamAssessmentGroup = mandatory {
    val template = new UpstreamAssessmentGroup
    template.academicYear = mandatory(academicYear)
    template.occurrence = mandatory(occurrence)
    template.moduleCode = assessmentComponent.moduleCode
    template.sequence = assessmentComponent.sequence
    template.assessmentGroup = assessmentComponent.assessmentGroup

    assessmentMembershipService.getUpstreamAssessmentGroup(template, eagerLoad = true)
  }

  @ModelAttribute("command")
  def command(
    @ModelAttribute("assessmentComponent") assessmentComponent: AssessmentComponent,
    @ModelAttribute("upstreamAssessmentGroup") upstreamAssessmentGroup: UpstreamAssessmentGroup,
  ): RecordAssessmentComponentMarksCommand.Command =
    RecordAssessmentComponentMarksCommand(assessmentComponent, upstreamAssessmentGroup, user)

  @GetMapping
  def viewMarks(@ModelAttribute("upstreamAssessmentGroup") upstreamAssessmentGroup: UpstreamAssessmentGroup): Mav = {
    val info = UpstreamAssessmentGroupInfo(
      upstreamAssessmentGroup,
      assessmentMembershipService.getCurrentUpstreamAssessmentGroupMembers(upstreamAssessmentGroup.id)
    )

    val studentMarkRecords = ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService, resitService, assessmentMembershipService)

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "students" -> studentMarkRecords.map(jsonStudentMarkRecord)
    )))
  }

  def jsonStudentMarkRecord(student: StudentMarkRecord): Map[String, Any] = Map(
    "universityId" -> student.universityId,
    "currentMember" -> student.currentMember,
    "resitStudent" -> student.isReassessment,
    "mark" -> student.mark,
    "grade" -> student.grade,
    "needsWritingToSits" -> student.needsWritingToSits,
    "outOfSync" -> student.outOfSync,
    "sitsWriteError" -> student.sitsWriteError,
    "agreed" -> student.agreed,
  )

  @PostMapping(consumes = Array("application/json"))
  def recordMarks(
    @RequestBody request: RecordComponentMarksRequest,
    @ModelAttribute("command") command: RecordAssessmentComponentMarksCommand.Command,
    errors: Errors,
    @ModelAttribute("upstreamAssessmentGroup") upstreamAssessmentGroup: UpstreamAssessmentGroup,
  ): Mav = {
    request.copyTo(command, errors)
    globalValidator.validate(command, errors)
    command.validate(errors)

    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      command.apply()
      viewMarks(upstreamAssessmentGroup)
    }
  }

}

class RecordComponentMarksRequest extends JsonApiRequest[RecordAssessmentComponentMarksCommand.Command] {
  @BeanProperty var students: JList[RecordComponentMarkItem] = _

  override def copyTo(command: RecordAssessmentComponentMarksCommand.Command, errors: Errors): Unit = {
    command.students.clear()
    students.asScala.filter(_.universityId.hasText).foreach { item =>
      val s = new StudentMarksItem(item.universityId)
      s.resitSequence = item.resitSequence.maybeText.getOrElse("")
      s.mark = item.mark
      s.grade = item.grade
      s.comments = item.comments

      command.students.put(item.universityId, s)
    }
  }
}

class RecordComponentMarkItem {
  @BeanProperty var universityId: String = _
  @BeanProperty var resitSequence: String = _
  @BeanProperty var mark: String = _
  @BeanProperty var grade: String = _
  @BeanProperty var comments: String = _
}
