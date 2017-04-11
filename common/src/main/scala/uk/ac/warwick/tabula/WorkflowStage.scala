package uk.ac.warwick.tabula

import scala.collection.immutable.ListMap

case class WorkflowProgress(
  percentage: Int,
  messageCode: String,
  cssClass: String,
  nextStage: Option[WorkflowStage],
  stages: ListMap[String, WorkflowStages.StageProgress]
)

abstract class WorkflowStage {
	def actionCode: String

	// Returns a sequence of a sequence of workflows; at least one of the inner sequence must have all been fulfilled.
	// So for an AND, you might just do Seq(Seq(stage1, stage2, stage3)) but for an OR you can do Seq(Seq(stage1), Seq(stage2))
	def preconditions: Seq[Seq[WorkflowStage]] = Seq()
}

sealed abstract class WorkflowStageHealth(val cssClass: String)
object WorkflowStageHealth {
	case object Good extends WorkflowStageHealth("success")
	case object Warning extends WorkflowStageHealth("warning")
	case object Danger extends WorkflowStageHealth("danger")

	// lame manual collection. Keep in sync with the case objects above
	val members = Set(Good, Warning, Danger)

	def fromCssClass(cssClass: String): WorkflowStageHealth =
		if (cssClass == null) null
		else members.find{_.cssClass == cssClass} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

object WorkflowStages {
	case class StageProgress(
		stage: WorkflowStage,
		started: Boolean,
		messageCode: String,
		health: WorkflowStageHealth=WorkflowStageHealth.Good,
		completed: Boolean=false,
		preconditionsMet: Boolean=false
	)

	def toMap(progresses: Seq[StageProgress]): ListMap[String, StageProgress] = {
		val builder = ListMap.newBuilder[String, StageProgress]

		def preconditionsMet(p: StageProgress) =
			if (p.stage.preconditions.isEmpty) true
			// For each item in at least one predicate, we have completed
			else p.stage.preconditions.exists { predicate => predicate.forall { stage =>
				progresses.find(_.stage == stage) match {
					case Some(progress) if progress.completed => true
					case _ => false
				}
			}}

		// We know at this point whether all the preconditions have been met
		builder ++= (progresses map { p =>
			p.stage.toString -> StageProgress(
				stage=p.stage,
				started=p.started,
				messageCode=p.messageCode,
				health=p.health,
				completed=p.completed,
				preconditionsMet=preconditionsMet(p)
			)
		})

		builder.result()
	}
}