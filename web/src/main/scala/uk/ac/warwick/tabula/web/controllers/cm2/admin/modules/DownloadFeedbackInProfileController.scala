package uk.ac.warwick.tabula.web.controllers.cm2.admin.modules

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.cm2.feedback.DownloadFeedbackCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Member}
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.services.fileserver.{ContentDisposition, RenderableFile}
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Controller
@RequestMapping(value = Array("/coursework/submission/{assignment}/{student}"))
class DownloadFeedbackInProfileController extends CourseworkController {

  var feedbackService: FeedbackService = Wire[FeedbackService]

  @ModelAttribute("downloadFeedbackCommand")
  def command(@PathVariable assignment: Assignment, @PathVariable student: Member): DownloadFeedbackCommand.Command =
    DownloadFeedbackCommand(assignment, mandatory(feedbackService.getFeedbackByUsercode(assignment, student.userId).filter(_.released)), Some(student))

  @RequestMapping(value = Array("/all/feedback.zip"))
  def getAll(@ModelAttribute("downloadFeedbackCommand") command: DownloadFeedbackCommand.Command, @PathVariable student: Member): RenderableFile =
    getOne(command)

  @RequestMapping(value = Array("/get/{filename}"))
  def getOne(@ModelAttribute("downloadFeedbackCommand") command: DownloadFeedbackCommand.Command): RenderableFile =
    command.apply().map(_.withContentDisposition(ContentDisposition.Attachment))
      .getOrElse(throw new ItemNotFoundException)

}



