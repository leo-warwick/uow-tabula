package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Module, Feedback, Assignment}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.util.StringUtils

class AdminAddMarksCommand(module:Module, assignment: Assignment, submitter: CurrentUser)
	extends AddMarksCommand[List[Feedback]](module, assignment, submitter){

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Marks.Create, assignment)

	override def checkMarkUpdated(mark: MarkItem) {
		// Warn if marks for this student are already uploaded
		assignment.feedbacks.find { (feedback) => feedback.universityId == mark.universityId && (feedback.hasMark || feedback.hasGrade) } match {
			case Some(feedback) => {
				val markChanged = feedback.actualMark match {
					case Some(m) if m.toString != mark.actualMark => true
					case _ => false
				}
				val gradeChanged = feedback.actualGrade match {
					case Some(g) if g != mark.actualGrade => true
					case _ => false
				}
				if (markChanged || gradeChanged){
					mark.isModified = true
					mark.isPublished = feedback.released
				}
			}
			case None => {}
		}
	}

	override def applyInternal(): List[Feedback] = transactional() {
		def saveFeedback(universityId: String, actualMark: String, actualGrade: String, isModified: Boolean) = {
			val feedback = assignment.findFeedback(universityId).getOrElse({
				val newFeedback = new Feedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = submitter.apparentId
				newFeedback.universityId = universityId
				newFeedback.released = false
				newFeedback
			})
			feedback.actualMark = StringUtils.hasText(actualMark) match {
				case true => Some(actualMark.toInt)
				case false => None
			}
			feedback.actualGrade = Option(actualGrade)
			session.saveOrUpdate(feedback)

			if (feedback.released && isModified){
				transactional() {
					val student = userLookup.getUserByWarwickUniId(feedback.universityId)
					val notifyMarkChanged = new FeedbackChangeNotifyCommand(module, assignment, student)
					notifyMarkChanged.apply()
				}
			}

			feedback
		}

		// persist valid marks
		val markList = marks filter (_.isValid) map {
			(mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade, mark.isModified)
		}

		markList.toList
	}

}
