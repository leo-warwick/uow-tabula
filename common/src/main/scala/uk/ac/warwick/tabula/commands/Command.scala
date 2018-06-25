package uk.ac.warwick.tabula.commands

import org.slf4j.{Logger, LoggerFactory}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.data.model.triggers.Trigger
import uk.ac.warwick.tabula.events._
import uk.ac.warwick.tabula.helpers.Stopwatches.StopWatch
import uk.ac.warwick.tabula.helpers.{Logging, Promise, Promises}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, CannotPerformWriteOperationException}
import uk.ac.warwick.tabula.system.permissions.{PerformsPermissionsChecking, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, DateFormats, JavaImports, RequestInfo}
import uk.ac.warwick.userlookup.User

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
	def emit(result: A): Seq[Notification[_, _]]
}


trait SchedulesNotifications[A, B] {
	def transformResult(commandResult: A): Seq[B]
	def scheduledNotifications(notificationTarget: B): Seq[ScheduledNotification[_]]
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

	def readOnlyTransaction = false

	def withTransaction(f: => A): A = transactional(readOnlyTransaction)(f)

	final def apply(): A =
		if (EventHandling.enabled) {
			if (readOnlyCheck(this)) {
				recordEvent(this) {
					withTransaction {
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
				throw maintenanceModeService.exception(this)
			} else {
				throw new CannotPerformWriteOperationException(this)
			}
		} else {
			handleTriggers(this) { notify(this) { benchmark() { applyInternal() } } }
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

	lazy val eventName: String = {
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

trait CommandWithoutTransaction[A] extends Command[A] {

	override final def withTransaction(f: => A): A = f

}

abstract class PromisingCommand[A] extends Command[A] with Promise[A] {
	private var _promise = Promises.promise[A]

	final def get: A = promisedValue
	final def promisedValue: A = _promise.get
	final def promisedValue_=(value: => A): A = {
		_promise.set(value)
		value
	}
}

object Command {
	val MillisToSlowlog = 5000
	val slowLogger: Logger = LoggerFactory.getLogger("uk.ac.warwick.tabula.Command.SLOW_LOG")

	// TODO this will break if we start doing stuff in parallols
	private val threadLocal = new ThreadLocal[Option[uk.ac.warwick.util.core.StopWatch]] {
		override def initialValue = None
	}

	def getOrInitStopwatch(): uk.ac.warwick.util.core.StopWatch =
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
		if (currentStopwatch.isEmpty) {
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
trait ReadOnly { self: Command[_] =>
  override def readOnlyTransaction = true
}

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
	protected var map: Map[String, Any] = Map[String, Any]()
	def properties(props: (String, Any)*): Description = {
		map ++= props
		this
	}
	def properties(otherMap: Map[String, Any]): Description = {
		map ++= otherMap
		this
	}
	def property(prop: (String, Any)): Description = {
		map += prop
		this
	}

	/**
	 * Record a Feedback item, plus its assignment, module, department
	 */
	def feedback(feedback: Feedback): Description = {
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
	def submission(submission: Submission): Description = {
		property("submission" -> submission.id)
		if (submission.assignment != null) assignment(submission.assignment)
		this
	}

	/**
	 * University IDs
	 */
	def studentIds(universityId: String): Description = studentIds(Seq(universityId))
	def studentIds(universityIds: Seq[String]): Description = property("students" -> universityIds)

	/**
	 * Usercodes
	 */
	def studentUsercodes(usercode: String): Description = studentUsercodes(Seq(usercode))
	def studentUsercodes(usercodes: Seq[String]): Description = property("studentUsercodes" -> usercodes)

	def users(users: Seq[User]): Description = property("users" -> users.map(_.getUserId))

	/**
	 * List of Submissions IDs
	 */
	def submissions(submissions: Seq[Submission]): Description = property("submissions" -> submissions.map(_.id))

	/**
	  * List of Feedback IDs
	  */
	def feedbacks(feedbacks: Seq[Feedback]): Description = property("feedbacks" -> feedbacks.map(_.id))

	def fileAttachments(attachments: Seq[FileAttachment]): Description = property("attachments" -> attachments.map(a =>
		s"${a.id} - ${a.hash}"
	))

	def assessment(assessment: Assessment): Description = assessment match {
		case a: Assignment => assignment(a)
		case e: Exam => exam(e)
	}

	/**
	 * Record assignment, plus its module and department if available.
	 */
	def assignment(assignment: Assignment): Description = {
		property("assignment" -> assignment.id)
		if (assignment.module != null) module(assignment.module)
		this
	}

	def exam(exam: Exam): Description = {
		property("exam" -> exam.id)
		if (exam.module != null) module(exam.module)
		this
	}

	/**
	 * Record meeting, plus its creator and relationship type if available.
	 */
	def meeting(meeting: AbstractMeetingRecord): Description = {
		property("meeting" -> meeting.id)
		if (meeting.creator != null) member(meeting.creator)
		if (meeting.relationship != null) property("relationship" -> meeting.relationship.relationshipType.toString())
		this
	}

	/**
	 * Record member note, plus its student.
	 */
	def memberNote(memberNote: MemberNote): Description = {
		property("membernote" -> memberNote.id)
		if (memberNote.member != null) member(memberNote.member)
		this
	}

	/**
		* Record extenuating circumstances, plus the student.
		*/
	def extenuatingCircumstances(circumstances: ExtenuatingCircumstances): Description = {
		property("circumstances" -> circumstances.id)
		if (circumstances.member != null) member(circumstances.member)
		this
	}

	/**
	 * Record small group set, plus its module and department if available.
	 */
	def smallGroupSet(smallGroupSet: SmallGroupSet): Description = {
		property("smallGroupSet" -> smallGroupSet.id)
		if (smallGroupSet.module != null) module(smallGroupSet.module)
		this
	}

	/**
	 * Record small group set, plus its department if available.
	 */
	def departmentSmallGroupSet(smallGroupSet: DepartmentSmallGroupSet): Description = {
		property("smallGroupSet" -> smallGroupSet.id)
		if (smallGroupSet.department != null) department(smallGroupSet.department)
		this
	}

  /**
   * Record a collection of SmallGroupSets
   */
  def smallGroupSetCollection(smallGroupSets: Seq[SmallGroupSet]): Description = {
    property("smallGroupSets" -> smallGroupSets.map(_.id).mkString(","))
    this
  }

  /**
	 * Record small group, plus its set, module and department if available.
	 */
	def smallGroup(smallGroup: SmallGroup): Description = {
		property("smallGroup" -> smallGroup.id)
		if (smallGroup.groupSet != null) smallGroupSet(smallGroup.groupSet)
		this
	}

	/**
	 * Record small group, plus its set and department if available.
	 */
	def departmentSmallGroup(departmentSmallGroup: DepartmentSmallGroup): Description = {
		property("smallGroup" -> departmentSmallGroup.id)
		if (departmentSmallGroup.groupSet != null) departmentSmallGroupSet(departmentSmallGroup.groupSet)
		this
	}

	/**
	 * Record small group event, plus its group, set, module and department if available.
	 */
	def smallGroupEvent(smallGroupEvent: SmallGroupEvent): Description = {
		property("smallGroupEvent" -> smallGroupEvent.id)
		if (smallGroupEvent.group != null) smallGroup(smallGroupEvent.group)
		this
	}

	/**
	 * Record small group event occurrence, plus its event, group, set, module and department if available.
	 */
	def smallGroupEventOccurrence(smallGroupEventOccurrence: SmallGroupEventOccurrence): Description = {
		property("smallGroupEventOccurrence" -> smallGroupEventOccurrence.id)
		if (smallGroupEventOccurrence.event != null) smallGroupEvent(smallGroupEventOccurrence.event)
		this
	}

	def markingWorkflow(scheme: MarkingWorkflow): Description = {
		property("markingWorkflow" -> scheme.id)
	}

	def markingWorkflow(markingWorkflow: CM2MarkingWorkflow): Description = {
		property("markingWorkflow" -> markingWorkflow.id)
	}

	/**
	 * Record module, plus department.
	 */
	def module(module: Module): Description = {
		if (module.adminDepartment != null) department(module.adminDepartment)
		property("module" -> module.id)
	}

	def department(department: Department): Description = {
		property("department", department.code)
	}

	def member(member: Member): Description = {
		property("member", member.universityId)
	}

	def studentRelationshipType(relationshipType: StudentRelationshipType): Description = {
		property("studentRelationshipType", relationshipType.agentRole)
	}

	def route(route: Route): Description = {
		if (route.adminDepartment != null) department(route.adminDepartment)
		property("route", route.code)
	}

	def attendanceMonitoringScheme(scheme: AttendanceMonitoringScheme): Description = {
		property("attendanceMonitoringScheme", scheme.id)
	}

	def attendanceMonitoringSchemes(schemes: Seq[AttendanceMonitoringScheme]): Description = {
		property("attendanceMonitoringSchemes", schemes.map(_.id))
	}

	def attendanceMonitoringPoints(points: Seq[AttendanceMonitoringPoint], verbose: Boolean = false): Description = {
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

	def attendanceMonitoringTemplate(template: AttendanceMonitoringTemplate): Description = {
		property("attendanceMonitoringTemplate", template.id)
	}

	def attendanceMonitoringTemplatePoint(templatePoint: AttendanceMonitoringTemplatePoint): Description = {
		property("attendanceMonitoringTemplatePoint", templatePoint)
		attendanceMonitoringTemplate(templatePoint.scheme)
	}

	def notifications(notifications: Seq[Notification[_,_]]): Description = {
		property("notifications" -> notifications.map(_.id))
	}

	def customRoleDefinition(customRoleDefinition: CustomRoleDefinition): Description = {
		if (customRoleDefinition.department != null) department(customRoleDefinition.department)
		property("customRoleDefinition", customRoleDefinition.id)
	}

	def extensionState(state: ExtensionState): Description = {
		property("extensionState", state.description)
	}

	// delegate equality to the underlying map
	override def hashCode: Int = map.hashCode()
	override def equals(that: Any): Boolean = that match {
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
	def allProperties: Map[String, Any] = map
}

/**
 * Shims for doing cake-style composition of the guts of a specific command implementation with the
 *  frameworky stuff that Command itself implements
 */
trait CommandInternal[A] {
	protected def applyInternal(): A
}

trait PopulateOnForm {
	def populate(): Unit
}


trait ComposableCommand[A] extends Command[A] with PerformsPermissionsChecking {
	self: CommandInternal[A] with Describable[A] with RequiresPermissionsChecking =>
}

trait ComposableCommandWithoutTransaction[A] extends CommandWithoutTransaction[A] with PerformsPermissionsChecking {
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