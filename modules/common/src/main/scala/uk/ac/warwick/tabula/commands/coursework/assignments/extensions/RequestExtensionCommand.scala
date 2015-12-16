package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import uk.ac.warwick.tabula.data.model.notifications.coursework.{ExtensionRequestModifiedNotification, ExtensionRequestCreatedNotification}

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.Transactions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.mutable

object RequestExtensionCommand {
	def apply (module: Module, assignment: Assignment, submitter: CurrentUser, action: String) = {
		new RequestExtensionCommandInternal(module, assignment, submitter)
			with ComposableCommand[Extension]
			with AutowiringRelationshipServiceComponent
			with RequestExtensionCommandDescription
			with RequestExtensionCommandPermission
			with RequestExtensionCommandValidation
			with RequestExtensionCommandNotification
			with AutowiringExtensionServiceComponent
	}
}


class RequestExtensionCommandInternal(val module: Module, val assignment:Assignment, val submitter: CurrentUser)
	extends CommandInternal[Extension]
		with RequestExtensionCommandState
		with BindListener {

	self: RelationshipServiceComponent with ExtensionServiceComponent =>

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}

	override def applyInternal() = transactional() {
		val universityId = submitter.apparentUser.getWarwickId
		val extension = assignment.findExtension(universityId).getOrElse({
			val newExtension = new Extension(universityId)
			newExtension.userId = submitter.apparentUser.getUserId
			newExtension
		})
		extension.assignment = assignment
		extension.requestedExpiryDate = requestedExpiryDate
		extension.reason = reason
		extension.requestedOn = DateTime.now
		extension.disabilityAdjustment = Option(Boolean.unbox(disabilityAdjustment)).getOrElse(false)


		if (extension.attachments != null) {
			// delete attachments that have been removed
			val matchingAttachments: mutable.Set[FileAttachment] = extension.attachments -- attachedFiles
			matchingAttachments.foreach(extensionService.deleteAttachment(extension, _))
		}

		if (!file.attached.isEmpty) {
			for (attachment <- file.attached) {
				extension.addAttachment(attachment)
			}
		}

		extensionService.save(extension)
		extension
	}
}


trait RequestExtensionCommandValidation extends SelfValidating {
	self: RequestExtensionCommandState =>

	def validate(errors: Errors) {
		if(!submitter.apparentUser.getUserId.hasText){
			errors.reject("extension.noValidUserId")
		}

		if (!readGuidelines){
			errors.rejectValue("readGuidelines","extension.readGuidelines.mustConfirmRead" )
		}
		if (requestedExpiryDate == null){
			errors.rejectValue("requestedExpiryDate", "extension.requestedExpiryDate.provideExpiry")
		} else if(requestedExpiryDate.isBefore(assignment.closeDate)){
			errors.rejectValue("requestedExpiryDate", "extension.requestedExpiryDate.beforeAssignmentExpiry")
		}
		if (!reason.hasText){
			errors.rejectValue("reason", "extension.reason.provideReasons")
		}
		val hasValidExtension = assignment
			.findExtension(submitter.apparentUser.getWarwickId)
			.filter(_.approved)
			.flatMap(_.expiryDate)
			.exists(_.isAfterNow)

		if (!hasValidExtension && !assignment.newExtensionsCanBeRequested) {
			errors.reject("assignment.extensionRequests.disallowed")
		}
	}
}


trait RequestExtensionCommandPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: RequestExtensionCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.Extension.MakeRequest, assignment)
	}
}


trait RequestExtensionCommandDescription extends Describable[Extension] {
	self: RequestExtensionCommandState =>

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
	}

	override def describeResult(d: Description, extension: Extension) {
		d.assignment(assignment)
			.property("extension" -> extension.id)
			.fileAttachments(extension.attachments.toSeq)
	}
}


trait RequestExtensionCommandNotification extends Notifies[Extension, Option[Extension]] {
	self: RequestExtensionCommandState with RelationshipServiceComponent =>

	val basicInfo = Map("moduleManagers" -> module.managers.users)
	val studentRelationships = relationshipService.allStudentRelationshipTypes
	val extraInfo = basicInfo ++ submitter.profile.collect { case student: StudentMember => student }.flatMap { _.mostSignificantCourseDetails.map { scd =>
		val relationships = studentRelationships.map(x => (x.description, relationshipService.findCurrentRelationships(x,scd.student))).toMap

		//Pick only the parts of scd required since passing the whole object fails due to the session not being available to load lazy objects
		Map(
			"relationships" -> relationships.filter({case (relationshipType,relations) => relations.nonEmpty}),
			"scdCourse" -> scd.course,
			"scdRoute" -> scd.route,
			"scdAward" -> scd.award
		)
	}}.getOrElse(Map())

	def emit(extension: Extension) = {
		val agent = submitter.apparentUser
		val assignment = extension.assignment
		val baseNotification = if (modified){
			new ExtensionRequestModifiedNotification
		} else {
			new ExtensionRequestCreatedNotification
		}
		Seq(Notification.init(baseNotification, agent, Seq(extension), assignment))
	}
}


trait RequestExtensionCommandState {
	val module: Module
	val assignment:Assignment
	val submitter: CurrentUser

	def presetValues(extension: Extension) {
		reason = extension.reason
		requestedExpiryDate = extension.requestedExpiryDate.orNull
		attachedFiles = extension.attachments
		modified = true
		disabilityAdjustment = extension.disabilityAdjustment
	}

	var reason: String =_

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@WithinYears(maxFuture = 3)
	var requestedExpiryDate: DateTime =_

	var file: UploadedFile = new UploadedFile
	var attachedFiles: JSet[FileAttachment] = JSet()
	@beans.BeanProperty var readGuidelines: JBoolean = false
	// true if this command is modifying an existing extension. False otherwise
	var modified: JBoolean = false
	var disabilityAdjustment: JBoolean = false
}
