package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import javax.servlet.http.HttpServletResponse
import org.joda.time.{LocalDate, LocalTime}
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{AssessmentMembershipInfoToJsonConverter, AssignmentToJsonConverter, AssignmentToXmlConverter}
import uk.ac.warwick.tabula.commands.cm2.assignments.{CreateAssignmentMonolithCommand, CreateAssignmentMonolithRequest, ModifyAssignmentMonolithRequest, SharedAssignmentStudentProperties}
import uk.ac.warwick.tabula.commands.{UpstreamGroup, UpstreamGroupPropertyEditor, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.helpers.XmlUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView, XmlErrorView, XmlView}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

abstract class ModuleAssignmentsController extends ApiController
  with AssignmentToJsonConverter
  with AssignmentToXmlConverter
  with AssessmentMembershipInfoToJsonConverter

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments"))
class ListAssignmentsForModuleController extends ModuleAssignmentsController {

  @ModelAttribute("listCommand")
  def command(@PathVariable module: Module, user: CurrentUser): ViewViewableCommand[Module] =
    new ViewViewableCommand(Permissions.Module.ManageAssignments, mandatory(module))

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def list(@ModelAttribute("listCommand") command: ViewViewableCommand[Module], errors: Errors, @RequestParam(required = false) academicYear: AcademicYear): Mav = {
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val module = command.apply()
      val assignments = module.assignments.asScala.filter { assignment =>
        !assignment.deleted && (academicYear == null || academicYear == assignment.academicYear)
      }

      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "academicYear" -> Option(academicYear).map(_.toString).orNull,
        "assignments" -> assignments.map(jsonAssignmentObject(_))
      )))
    }
  }

  @RequestMapping(method = Array(GET), produces = Array("application/xml"))
  def listXML(@ModelAttribute("listCommand") command: ViewViewableCommand[Module], errors: Errors, @RequestParam(required = false) academicYear: AcademicYear): Mav = {
    if (errors.hasErrors) {
      Mav(new XmlErrorView(errors))
    } else {
      val module = command.apply()
      val assignments = module.assignments.asScala.filter { assignment =>
        !assignment.deleted && (academicYear == null || academicYear == assignment.academicYear)
      }

      Mav(new XmlView(
        <assignments>
          {assignments.map(xmlAssignmentObject)}
        </assignments> % Map(
          "success" -> true,
          "status" -> "ok",
          "academicYear" -> Option(academicYear).map(_.toString).orNull
        )
      ))
    }
  }
}

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments"))
class CreateAssignmentController extends ModuleAssignmentsController {

  @ModelAttribute("createCommand")
  def command(@PathVariable module: Module): CreateAssignmentMonolithCommand.Command =
    CreateAssignmentMonolithCommand(module)

  @InitBinder(Array("createCommand"))
  def upstreamGroupBinder(binder: WebDataBinder): Unit = {
    binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
  }

  @RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
  def create(@RequestBody request: CreateAssignmentRequest, @ModelAttribute("createCommand") command: CreateAssignmentMonolithCommand.Command, errors: Errors)(implicit response: HttpServletResponse): Mav = {
    request.copyTo(command, errors)

    globalValidator.validate(command, errors)
    command.validate(errors)
    command.afterBind()

    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val assignment = command.apply()

      response.setStatus(HttpStatus.CREATED.value())
      response.addHeader("Location", toplevelUrl + Routes.api.assignment(assignment))

      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "assignment" -> jsonAssignmentObject(assignment)
      )))
    }
  }
}

case class SitsLink(moduleCode: String, occurrence: String, sequence: String)


trait AssignmentPropertiesRequest[A <: ModifyAssignmentMonolithRequest] extends JsonApiRequest[A]
  with BooleanAssignmentProperties with SharedAssignmentStudentProperties {

  @BeanProperty var name: String = null
  @BeanProperty var openDate: LocalDate = null
  @BeanProperty var closeDate: LocalDate = null
  @BeanProperty var closeTime: LocalTime = null
  @BeanProperty var academicYear: AcademicYear = null
  @BeanProperty var feedbackTemplate: FeedbackTemplate = null
  @BeanProperty var markingWorkflow: CM2MarkingWorkflow = null
  @BeanProperty var includeUsers: JList[String] = null
  @BeanProperty var excludeUsers: JList[String] = null
  @BeanProperty var upstreamGroups: JList[UpstreamGroup] = null
  @BeanProperty var sitsLinks: JList[SitsLink] = null
  @BeanProperty var fileAttachmentLimit: JInteger = null
  @BeanProperty var unlimitedAttachments: JBoolean = null
  @BeanProperty var fileAttachmentTypes: JList[String] = null
  @BeanProperty var individualFileSizeLimit: JInteger = null
  @BeanProperty var minWordCount: JInteger = null
  @BeanProperty var maxWordCount: JInteger = null
  @BeanProperty var wordCountConventions: String = null

  override def copyTo(state: A, errors: Errors): Unit = {
    if (Option(openDate).isEmpty && Option(closeDate).nonEmpty) {
      if (openEnded) openDate = LocalDate.now
      else openDate = closeDate.minusWeeks(2)
    }

    Option(name).foreach(state.name = _)
    Option(openDate).foreach(state.openDate = _)
    Option(closeDate).foreach(d => state.closeDate = d.toDateTime(Option(closeTime).getOrElse(Assignment.defaultCloseTime)))

    state match {
      case createState: CreateAssignmentMonolithRequest =>
        Option(academicYear).foreach(createState.academicYear = _)

      // Don't allow editing the academic year
      case _ => require(academicYear == null || academicYear == state.academicYear)
    }

    Option(feedbackTemplate).foreach(state.feedbackTemplate = _)
    Option(includeUsers).foreach { list => state.massAddUsers = list.asScala.mkString("\n") }
    Option(excludeUsers).foreach { state.excludeUsers = _ }
    Option(fileAttachmentLimit).foreach(state.fileAttachmentLimit = _)
    Option(unlimitedAttachments).foreach(state.unlimitedAttachments = _)
    Option(fileAttachmentTypes).foreach(state.fileAttachmentTypes = _)
    Option(individualFileSizeLimit).foreach(state.individualFileSizeLimit = _)
    Option(minWordCount).foreach(state.wordCountMin = _)
    Option(maxWordCount).foreach(state.wordCountMax = _)
    Option(wordCountConventions).foreach(state.wordCountConventions = _)
    Option(openEnded).foreach(state.openEnded = _)
    Option(collectMarks).foreach(state.collectMarks = _)
    Option(collectSubmissions).foreach(state.collectSubmissions = _)
    Option(restrictSubmissions).foreach(state.restrictSubmissions = _)
    Option(allowLateSubmissions).foreach(state.allowLateSubmissions = _)
    Option(allowResubmission).foreach(state.allowResubmission = _)
    Option(displayPlagiarismNotice).foreach(state.displayPlagiarismNotice = _)
    Option(allowExtensions).foreach(state.allowExtensions = _)
    Option(extensionAttachmentMandatory).foreach(state.extensionAttachmentMandatory = _)
    Option(allowExtensionsAfterCloseDate).foreach(state.allowExtensionsAfterCloseDate = _)
    Option(summative).foreach(state.summative = _)
    Option(dissertation).foreach(state.dissertation = _)
    Option(publishFeedback).foreach(state.publishFeedback = _)
    Option(includeInFeedbackReportWithoutSubmissions).foreach(state.includeInFeedbackReportWithoutSubmissions = _)
    Option(automaticallyReleaseToMarkers).foreach(state.automaticallyReleaseToMarkers = _)
    Option(automaticallySubmitToTurnitin).foreach(state.automaticallySubmitToTurnitin = _)
    Option(turnitinStoreInRepository).foreach(state.turnitinStoreInRepository = _)
    Option(turnitinExcludeBibliography).foreach(state.turnitinExcludeBibliography = _)
    Option(turnitinExcludeQuoted).foreach(state.turnitinExcludeQuoted = _)
    Option(hiddenFromStudents).foreach(state.hiddenFromStudents = _)
    Option(resitAssessment).foreach(state.resitAssessment = _)
    Option(anonymity).foreach(state.anonymity = _)
    Option(createdByAEP).foreach(state.createdByAEP = _)

    if (sitsLinks != null ) {
      val sitsLinksSeq = Option(sitsLinks.asScala).getOrElse(Seq.empty)
      if (sitsLinksSeq.forall { link =>
        state.allUpstreamGroups.exists(uag => uag.assessmentComponent.moduleCode == link.moduleCode
          && uag.assessmentComponent.sequence == link.sequence && uag.occurrence == link.occurrence)

      }) {
        val linkedUpstreamGroups: JList[UpstreamGroup] = state.allUpstreamGroups.filter { upstreamGroup =>
          sitsLinksSeq.exists { sitsLink =>
            upstreamGroup.assessmentComponent.moduleCode == sitsLink.moduleCode &&
              upstreamGroup.sequence == sitsLink.sequence &&
              upstreamGroup.occurrence == sitsLink.occurrence
          }

        }.asJava
        state.upstreamGroups = linkedUpstreamGroups
      } else {
        errors.reject("assignment.api.sitsLinks.invalidData")
      }
    }
    state.afterBind()
  }

}

class CreateAssignmentRequest extends AssignmentPropertiesRequest[CreateAssignmentMonolithRequest] {

  // Default values
  includeUsers = JArrayList()
  upstreamGroups = JArrayList()
  fileAttachmentLimit = 1
  unlimitedAttachments = false
  fileAttachmentTypes = JArrayList()
  wordCountConventions = "Exclude any bibliography or appendices."
  createdByAEP = false

}
