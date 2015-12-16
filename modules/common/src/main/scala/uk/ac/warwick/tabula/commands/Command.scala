package uk.ac.warwick.tabula.commands

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.data.model.triggers.Trigger
import uk.ac.warwick.tabula.events._
import uk.ac.warwick.tabula.helpers.{Logging, Promise, Promises}
import uk.ac.warwick.tabula.helpers.Stopwatches.StopWatch
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, CannotPerformWriteOperationException, MaintenanceModeService}
import uk.ac.warwick.tabula.system.permissions.{PerformsPermissionsChecking, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{DateFormats, AutowiringFeaturesComponent, JavaImports, RequestInfo}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.queue.Queue
import collection.mutable.ArrayBuffer

/**
 * Trait for a thing that can describe itself to a Description
 * object. You can put arbitrary properties into Description but
 * it's always best to use a dedicated method, e.g. assignment(), to
 * make things more maintainable. assignment() will automatically
 * record its module and department info.
 */
trait BaseDescribable[A] {
	// describe the thing that's happening.
	def describe(d: Description)
	// optional extra description after the thing's happened.
	def describeResult(d: Description, result: A) { describeResult(d) }
	def describeResult(d: Description) {}
}

trait Describable[A] extends BaseDescribable[A] with KnowsEventName

// Broken out from describable so that we can write classes which just implement describe
trait KnowsEventName {
	val eventName: String
}

/**
 * Takes an A (usually the result of a Command) and generates notifications for Bs. Often, A == B.
 */
trait Notifies[A, B] {
	def emit(result: A): Seq[Notification[_ >: Null <: ToEntityReference, _]]
}


trait SchedulesNotifications[A, B] {
	def transformResult(commandResult: A): Seq[B]
	def scheduledNotifications(notificationTarget: B): Seq[ScheduledNotification[_ >: Null <: ToEntityReference]]
}

trait CompletesNotifications[A] {
	class CompletesNotificationsResult(val notifications: Seq[ActionRequiredNotification], val completedBy: User)
	object CompletesNotificationsResult {
		def apply(notifications: Seq[ActionRequiredNotification], completedBy: User) =
			new CompletesNotificationsResult(notifications, completedBy)
	}
	object EmptyCompletesNotificationsResult extends CompletesNotificationsResult(Nil, null)
	def notificationsToComplete(commandResult: A): CompletesNotificationsResult
}

trait GeneratesTriggers[A] {
	def generateTriggers(commandResult: A): Seq[Trigger[_ >: Null <: ToEntityReference, _]]
}

trait Appliable[A]{
  def apply(): A
}

/**
 * Stateful instance of an action in the application.
 * It could be anything that we might want to keep track of,
 * especially if we might want to audit log it. Anything that
 * adds or changes any data is a candidate. Read-only queries,
 * not so much (unless we're interested in when a thing is observed/downloaded).
 *
 * Commands should implement work(), and
 *
 * <h2>Renaming a Command</h2>
 *
 * Think before renaming a command - by default the class name (minus "Command") is
 * used as the event name in audit trails, so if you rename it the audit events will
 * change name too. Careful now!
 */
trait Command[A] extends Describable[A] with Appliable[A]
		with JavaImports with EventHandling with NotificationHandling with TriggerHandling
		with PermissionsChecking with TaskBenchmarking with AutowiringFeaturesComponent with AutowiringMaintenanceModeServiceComponent {

	val deferredMessages: ArrayBuffer[Object] = ArrayBuffer()
	var messageQueue: Queue = Wire.named[Queue]("indexTopic")

	final def apply(): A = {
		val result = {
			if (EventHandling.enabled) {
				if (readOnlyCheck(this)) {
					recordEvent(this) {
						transactional() {
							handleTriggers(this) {
								notify(this) {
									benchmark() {
										applyInternal()
									}
								}
							}
						}
					}
				} else if (maintenanceModeService.enabled) {
					throw maintenanceModeService.exception(Some(this))
				} else {
					throw new CannotPerformWriteOperationException(this)
				}
			} else {
				handleTriggers(this) { notify(this) { benchmark() { applyInternal() } } }
			}
		}
		deferredMessages.foreach(messageQueue.send)
		result
	}

	private def benchmark()(fn: => A) = benchmarkTask(benchmarkDescription) { fn }

	private def benchmarkDescription = {
		val event = Event.fromDescribable(this)
		EventDescription.generateMessage(event, "command").toString()
	}

	/**
		Subclasses do their work in here.

		Classes using a command should NOT call this method! call apply().
		The method here is protected but subclasses can easily override it
		to be publicly visible, so there's little to stop you from calling it.
		TODO somehow stop this being callable
	*/
	protected def applyInternal(): A

	lazy val eventName = {
	  val name = getClass.getName.replaceFirst(".*\\.","").replaceFirst("Command.*","")
		if (name.contains("anon$")) {
			// This can currently happen quite easily with caked-up commands. This code should
			// try to work around that if possible, I'm just making it explode now so it's more obvious
			throw new IllegalStateException(s"Command name calculated incorrectly as $name")
		}
		name
	}

	private def isReadOnlyMasquerade =
		RequestInfo.fromThread.exists { info =>
			info.user.masquerading && !info.user.sysadmin && !features.masqueradersCanWrite
		}

	private def readOnlyCheck(callee: Describable[_]) = {
		callee.isInstanceOf[ReadOnly] || (!maintenanceModeService.enabled && !isReadOnlyMasquerade)
	}
}

abstract class PromisingCommand[A] extends Command[A] with Promise[A] {
	private var _promise = Promises.promise[A]

	final def get = promisedValue
	final def promisedValue = _promise.get
	final def promisedValue_=(value: => A) = {
		_promise.set(value)
		value
	}
}

object Command {
	val MillisToSlowlog = 5000
	val slowLogger = LoggerFactory.getLogger("uk.ac.warwick.tabula.Command.SLOW_LOG")

	// TODO this will break if we start doing stuff in parallols
	private val threadLocal = new ThreadLocal[Option[uk.ac.warwick.util.core.StopWatch]] {
		override def initialValue = None
	}

	def getOrInitStopwatch() =
		threadLocal.get match {
			case Some(sw) => sw
			case None =>
				val sw = StopWatch()
				threadLocal.set(Some(sw))
				sw
		}

	def endStopwatching() { threadLocal.remove() }

	def timed[A](fn: uk.ac.warwick.util.core.StopWatch => A): A = {
		val currentStopwatch = threadLocal.get
		if (!currentStopwatch.isDefined) {
			try {
				val sw = StopWatch()
				threadLocal.set(Some(sw))
				fn(sw)
			} finally {
				threadLocal.remove()
			}
		} else {
			fn(currentStopwatch.get)
		}
	}
}

/** See ApplyWithCallback[A] */
trait HasCallback[A] {
	var callback: (A) => Unit = _
}

/**
 * Defines a function property to be used as a callback, plus a convenience
 * version of `apply` that provides the callback and runs the command
 * simultaneously.
 *
 * It doesn't actually call the callback - you do that in your `apply` implementation.
 */
trait ApplyWithCallback[A] extends Command[A] with HasCallback[A] {
	def apply(fn: (A) => Unit): A = {
		callback = fn
		apply()
	}
}

/**
 * Trait for a command which has a `validate` method. Implementing this trait
 * doesn't actually make anything magic happen at the moment - you still have
 * to call the validate method yourself. It does provide a few shortcuts to the
 * validation methods to simplify validation code.
 */
trait SelfValidating {
	def validate(errors: Errors)
}

/**
 * Marks a command as being safe to use during maintenance mode (other than audit events
 * which are handled separately). If it doesn't directly update or insert into the database,
 * it is safe.
 */
trait ReadOnly

/**
 * A Describable (usually a Command) marked as Unaudited will not be recorded
 * by the audit log when it is applied. This should only really be for read-only
 * commands that make no database changes and are really uninteresting to log, like
 * viewing a list of items.
 */
trait Unaudited { self: Describable[_] =>
	// override describe() with nothing, since it'll never be used.
	override def describe(d: Description) {}
}

/**
 * Object for a Command to describe what it's just done.
 *
 * You can use the `properties` and `property` methods to add any
 * arbitrary properties, but it's highly recommended that you use the
 * dedicated methods such as `assignment` to record which assignment
 * the command is working on, and to define a new method if the
 * existing ones don't fulfil your needs.
 */
abstract class Description {
	protected var map = Map[String, Any]()
	def properties(props: (String, Any)*) = {
		map ++= props
		this
	}
	def properties(otherMap: Map[String, Any]) = {
		map ++= otherMap
		this
	}
	def property(prop: (String, Any)) = {
		map += prop
		this
	}

	/**
	 * Record a Feedback item, plus its assignment, module, department
	 */
	def feedback(feedback: Feedback) = {
		property("feedback" -> feedback.id)
		HibernateHelpers.initialiseAndUnproxy(feedback) match {
			case assignmentFeedback: AssignmentFeedback if assignmentFeedback.assignment != null => assignment(assignmentFeedback.assignment)
			case examFeedback: ExamFeedback if examFeedback.exam != null => exam(examFeedback.exam)
		}
		this
	}

	/**
	 * Record a Submission item, plus its assignment, module, department
	 */
	def submission(submission: Submission) = {
		property("submission" -> submission.id)
		if (submission.assignment != null) assignment(submission.assignment)
		this
	}

	/**
	 * University IDs
	 */
	def studentIds(universityIds: Seq[String]) = property("students" -> universityIds)

	/**
	 * List of Submissions IDs
	 */
	def submissions(submissions: Seq[Submission]) = property("submissions" -> submissions.map(_.id))

	def fileAttachments(attachments: Seq[FileAttachment]) = property("attachments" -> attachments.map(_.id))

	def assessment(assessment: Assessment) = assessment match {
		case a: Assignment => assignment(a)
		case e: Exam => exam(e)
	}

	/**
	 * Record assignment, plus its module and department if available.
	 */
	def assignment(assignment: Assignment) = {
		property("assignment" -> assignment.id)
		if (assignment.module != null) module(assignment.module)
		this
	}

	def exam(exam: Exam) = {
		property("exam" -> exam.id)
		if (exam.module != null) module(exam.module)
		this
	}

	/**
	 * Record meeting, plus its creator and relationship type if available.
	 */
	def meeting(meeting: AbstractMeetingRecord) = {
		property("meeting" -> meeting.id)
		if (meeting.creator != null) member(meeting.creator)
		if (meeting.relationship != null) property("relationship" -> meeting.relationship.relationshipType.toString())
		this
	}

	/**
	 * Record member note, plus its student.
	 */
	def memberNote(memberNote: MemberNote) = {
		property("membernote" -> memberNote.id)
		if (memberNote.member != null) member(memberNote.member)
		this
	}

	/**
	 * Record small group set, plus its module and department if available.
	 */
	def smallGroupSet(smallGroupSet: SmallGroupSet) = {
		property("smallGroupSet" -> smallGroupSet.id)
		if (smallGroupSet.module != null) module(smallGroupSet.module)
		this
	}

	/**
	 * Record small group set, plus its department if available.
	 */
	def departmentSmallGroupSet(smallGroupSet: DepartmentSmallGroupSet) = {
		property("smallGroupSet" -> smallGroupSet.id)
		if (smallGroupSet.department != null) department(smallGroupSet.department)
		this
	}

  /**
   * Record a collection of SmallGroupSets
   */
  def smallGroupSetCollection(smallGroupSets: Seq[SmallGroupSet]) = {
    property("smallGroupSets" -> smallGroupSets.map(_.id).mkString(","))
    this
  }

  /**
	 * Record small group, plus its set, module and department if available.
	 */
	def smallGroup(smallGroup: SmallGroup) = {
		property("smallGroup" -> smallGroup.id)
		if (smallGroup.groupSet != null) smallGroupSet(smallGroup.groupSet)
		this
	}

	/**
	 * Record small group, plus its set and department if available.
	 */
	def departmentSmallGroup(departmentSmallGroup: DepartmentSmallGroup) = {
		property("smallGroup" -> departmentSmallGroup.id)
		if (departmentSmallGroup.groupSet != null) departmentSmallGroupSet(departmentSmallGroup.groupSet)
		this
	}

	/**
	 * Record small group event, plus its group, set, module and department if available.
	 */
	def smallGroupEvent(smallGroupEvent: SmallGroupEvent) = {
		property("smallGroupEvent" -> smallGroupEvent.id)
		if (smallGroupEvent.group != null) smallGroup(smallGroupEvent.group)
		this
	}

	/**
	 * Record small group event occurrence, plus its event, group, set, module and department if available.
	 */
	def smallGroupEventOccurrence(smallGroupEventOccurrence: SmallGroupEventOccurrence) = {
		property("smallGroupEventOccurrence" -> smallGroupEventOccurrence.id)
		if (smallGroupEventOccurrence.event != null) smallGroupEvent(smallGroupEventOccurrence.event)
		this
	}

	def markingWorkflow(scheme: MarkingWorkflow) = {
		property("markingWorkflow" -> scheme.id)
	}

	/**
	 * Record module, plus department.
	 */
	def module(module: Module) = {
		if (module.adminDepartment != null) department(module.adminDepartment)
		property("module" -> module.id)
	}

	def department(department: Department) = {
		property("department", department.code)
	}

	def member(member: Member) = {
		property("member", member.universityId)
	}

	def studentRelationshipType(relationshipType: StudentRelationshipType) = {
		property("studentRelationshipType", relationshipType.agentRole)
	}

	def route(route: Route) = {
		if (route.adminDepartment != null) department(route.adminDepartment)
		property("route", route.code)
	}

	def monitoringPointSet(set: MonitoringPointSet) = {
		if (set.route != null) route(set.route)
		property("monitoringPointSet", set.id)
	}

	def monitoringPointSetTemplate(template: MonitoringPointSetTemplate) = {
		property("monitoringPointSetTemplate", template.id)
	}

	def monitoringPoint(monitoringPoint: MonitoringPoint) = {
		property("monitoringPoint", monitoringPoint.id)
	}

	def monitoringPointTemplate(monitoringPoint: MonitoringPointTemplate) = {
		property("monitoringPointTemplate", monitoringPoint.id)
	}

	def monitoringCheckpoint(monitoringPoint: MonitoringPoint) = {
		property("monitoringCheckpoint", monitoringPoint.id)
	}

	def attendanceMonitoringScheme(scheme: AttendanceMonitoringScheme) = {
		property("attendanceMonitoringScheme", scheme.id)
	}

	def attendanceMonitoringSchemes(schemes: Seq[AttendanceMonitoringScheme]) = {
		property("attendanceMonitoringSchemes", schemes.map(_.id))
	}

	def attendanceMonitoringPoints(points: Seq[AttendanceMonitoringPoint], verbose: Boolean = false) = {
		if (verbose) {
			property("attendanceMonitoringPoint", points.map(point => Map(
				"id" -> point.id,
				"name" -> point.name,
				"startDate" -> DateFormats.IsoDateTime.print(point.startDate),
				"endDate" -> DateFormats.IsoDateTime.print(point.endDate),
				"type" -> point.pointType.dbValue
			)))
		} else {
			property("attendanceMonitoringPoint", points.map(_.id))
		}
		attendanceMonitoringSchemes(points.map(_.scheme))
	}

	def attendanceMonitoringTemplate(template: AttendanceMonitoringTemplate) = {
		property("attendanceMonitoringTemplate", template.id)
	}

	def attendanceMonitoringTemplatePoint(templatePoint: AttendanceMonitoringTemplatePoint) = {
		property("attendanceMonitoringTemplatePoint", templatePoint)
		attendanceMonitoringTemplate(templatePoint.scheme)
	}

	def notifications(notifications: Seq[Notification[_,_]]) = {
		property("notifications" -> notifications.map(_.id))
	}

	def customRoleDefinition(customRoleDefinition: CustomRoleDefinition) = {
		if (customRoleDefinition.department != null) department(customRoleDefinition.department)
		property("customRoleDefinition", customRoleDefinition.id)
	}

	// delegate equality to the underlying map
	override def hashCode = map.hashCode()
	override def equals(that: Any) = that match {
		case d: Description => map.equals(d.map)
		case _ => false
	}
}

/**
 * Fully implements Description, adding an accessor
 * to the underlying properties map for the auditing
 * framework to use.
 */
class DescriptionImpl extends Description {
	def allProperties = map
}

/**
 * Shims for doing cake-style composition of the guts of a specific command implementation with the
 *  frameworky stuff that Command itself implements
 */
trait CommandInternal[A] {
	protected def applyInternal():A
}

trait PopulateOnForm {
	def populate(): Unit
}


trait ComposableCommand[A] extends Command[A] with PerformsPermissionsChecking {
	self: CommandInternal[A] with Describable[A] with RequiresPermissionsChecking =>
}

/**
 * ComposableCommands often need to include a user in their state, for notifications etc. Depending on this trait allows
 * that to be mocked easily for testing
 */
trait UserAware {
	val user:User
}

trait TaskBenchmarking extends Logging {
	protected final def benchmarkTask[A](description: String)(fn: => A): A = Command.timed { timer =>
		benchmark(description, level=Warn, minMillis=Command.MillisToSlowlog, stopWatch=timer, logger=Command.slowLogger)(fn)
	}
}