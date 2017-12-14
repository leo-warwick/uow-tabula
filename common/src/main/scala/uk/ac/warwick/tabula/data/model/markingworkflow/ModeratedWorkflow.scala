package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{ModerationMarker, ModerationModerator, SelectedModerationMarker, SelectedModerationModerator}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{ModeratedMarking, SelectedModeratedMarking}
import uk.ac.warwick.tabula.data.model.markingworkflow.ModeratedWorkflow.Settings
import uk.ac.warwick.tabula.data.model.markingworkflow.ModerationSampler.Moderator
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._

@Entity @DiscriminatorValue("Moderated")
class ModeratedWorkflow extends CM2MarkingWorkflow {
	def workflowType:MarkingWorkflowType = ModeratedMarking

	override def replaceMarkers(markers: Seq[Marker]*): Unit = {
		val (firstMarkers, moderators) = markers.toList match {
			case fm :: sm :: _ => (fm, sm)
			case _ => throw new IllegalArgumentException("Must add a list of markers and moderators")
		}

		replaceStageMarkers(ModerationMarker, firstMarkers)
		replaceStageMarkers(ModerationModerator, moderators)
	}

	def moderationSampler: ModerationSampler = getStringSetting(Settings.ModerationSampler) match {
		case Some(stringType) => ModerationSampler.fromCode(stringType)
		case None => ModerationSampler.Moderator // Default to the moderator being the sampler
	}

	def moderationSampler_= (sampler: ModerationSampler): Unit = { settings += (Settings.ModerationSampler -> Option(sampler).getOrElse(Moderator).code) }
}

object ModeratedWorkflow {
	def apply(name: String, department: Department, sampler: ModerationSampler, markers: Seq[User], moderators: Seq[User]): ModeratedWorkflow = {
		val workflow = new ModeratedWorkflow
		workflow.name = name
		workflow.department = department
		workflow.moderationSampler = sampler

		val markersStage = new StageMarkers
		markersStage.stage = ModerationMarker
		markersStage.workflow = workflow
		markers.foreach(markersStage.markers.add)

		val moderatorStage = new StageMarkers
		moderatorStage.stage = ModerationModerator
		moderatorStage.workflow = workflow
		moderators.foreach(moderatorStage.markers.add)

		workflow.stageMarkers = JList(markersStage, moderatorStage)
		workflow
	}

	object Settings {
		val ModerationSampler = "moderationSampler"
	}
}

@Entity @DiscriminatorValue("SelectedModerated")
class SelectedModeratedWorkflow extends ModeratedWorkflow {
	override def workflowType:MarkingWorkflowType = SelectedModeratedMarking
}

object SelectedModeratedWorkflow {
	def apply(name: String, department: Department, sampler: ModerationSampler, markers: Seq[User], moderators: Seq[User]): ModeratedWorkflow = {
		val workflow = new SelectedModeratedWorkflow
		workflow.name = name
		workflow.department = department
		workflow.moderationSampler = sampler

		val markersStage = new StageMarkers
		markersStage.stage = SelectedModerationMarker
		markersStage.workflow = workflow
		markers.foreach(markersStage.markers.add)

		val moderatorStage = new StageMarkers
		moderatorStage.stage = SelectedModerationModerator
		moderatorStage.workflow = workflow
		moderators.foreach(moderatorStage.markers.add)

		workflow.stageMarkers = JList(markersStage, moderatorStage)
		workflow
	}

	object Settings {
		val ModerationSampler = "moderationSampler"
	}
}

sealed abstract class ModerationSampler(val code: String) {
	def getCode: String = code
	override def toString: String = code
}

object ModerationSampler {
	case object Marker extends ModerationSampler("marker")
	case object Moderator extends ModerationSampler("moderator")
	case object Admin extends ModerationSampler("admin")

	// manual collection - keep in sync with the case objects above
	def members = Seq(Marker, Moderator, Admin)

	def fromCode(code: String): ModerationSampler =
		if (code == null) null
		else members.find{_.code == code} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class ModerationSamplerConverter extends TwoWayConverter[String, ModerationSampler] {
	override def convertRight(source: String): ModerationSampler = source.maybeText.map(ModerationSampler.fromCode).getOrElse(throw new IllegalArgumentException)
	override def convertLeft(source: ModerationSampler): String = Option(source).map { _.code }.orNull
}