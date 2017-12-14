<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.
TODO grab values from the Routes object in code, as that's pretty equivalent and
   we're repeating ourselves here. OR expose Routes directly.
-->
<#macro _u page context=cm2Context>
	<@url context=context page=page />
</#macro>

<#macro home><@_u page="/" /></#macro>
<#macro markerHome><@_u page="/marker" /></#macro>

<#macro depthome module academicYear="">
	<#if academicYear?has_content>
		<@_u page="/admin/department/${module.adminDepartment.code}/${academicYear.startYear?c}/?moduleFilters=Module(${module.code})#module-${module.code}" />
	<#else>
		<@_u page="/admin/department/${module.adminDepartment.code}/?moduleFilters=Module(${module.code})#module-${module.code}" />
	</#if>
</#macro>
<#macro departmenthome department academicYear="">
	<#if academicYear?has_content>
		<@_u page="/admin/department/${department.code}/${academicYear.startYear?c}" />
	<#else>
		<@_u page="/admin/department/${department.code}" />
	</#if>
</#macro>
<#macro modulehome module academicYear><@_u page="/admin/${module.code}/${academicYear.startYear?c}" /></#macro>
<#macro downloadSubmission submission filename><@_u page="/admin/assignments/${submission.assignment.id}/submissions/download/${submission.id}/${filename?url}"/></#macro>

<#macro filterExtensions academicYear="">
	<#if academicYear?has_content>
		<@_u page="/admin/extensions/${academicYear.startYear?c}"/>
	<#else>
		<@_u page="/admin/extensions"/>
	</#if>
</#macro>
<#macro extensionDetail extension><@_u page="/admin/extensions/${extension.id}/detail"/></#macro>
<#macro extensiondetail assignment usercode><@_u page="/admin/assignments/${assignment.id}/extensions/${usercode}/detail" /></#macro>
<#macro extensionUpdate extension><@_u page="/admin/extensions/${extension.id}/update"/></#macro>
<#macro extensionAttachment extension filename><@_u page="/admin/extensions/${extension.id}/supporting-file/${filename?url}" /></#macro>
<#macro extensionSettings department><@_u page="/admin/department/${department.code}/settings/extensions" /></#macro>

<#macro reusableWorkflowsHome department academicYear="">
	<#if academicYear?has_content>
		<@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows" />
	<#else>
		<@_u page="/admin/department/${department.code}/markingworkflows" />
	</#if>
</#macro>
<#macro reusableWorkflowAdd department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/add" /></#macro>
<#macro reusableWorkflowAddToCurrentYear department academicYear workflow><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/${workflow.id}/copy" /></#macro>
<#macro reusableWorkflowEdit department academicYear workflow><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/${workflow.id}/edit" /></#macro>
<#macro reusableWorkflowDelete department academicYear workflow><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/${workflow.id}/delete" /></#macro>
<#macro reusableWorkflowReplaceMarker department academicYear workflow><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/${workflow.id}/replace" /></#macro>

<#macro feedbackreport department academicYear="">
	<#if academicYear?has_content>
		<@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/reports/feedback"/>
	<#else>
		<@_u page="/admin/department/${department.code}/reports/feedback"/>
	</#if>
</#macro>

<#macro createassignmentdetails module academicYear><@_u page="/admin/${module.code}/${academicYear.startYear?c}/assignments/new" /></#macro>
<#macro assignmentfeedback assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/feedback" /></#macro>
<#macro assignmentstudents assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/students" /></#macro>
<#macro assignmentmarkers assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/markers" /></#macro>
<#macro assignmentmarkerssmallgroups assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/markers/smallgroups" /></#macro>
<#macro assignmentmarkerstemplate assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/markers/template" /></#macro>
<#macro assignmentmarkerstemplatedownload assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/markers/template/download" /></#macro>

<#macro assignmentsubmissions assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/submissions" /></#macro>
<#macro assignmentoptions assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/options" /></#macro>
<#macro assignmentreview assignment><@_u page="/admin/assignments/${assignment.id}/review" /></#macro>
<#macro assignmentrequestaccess assignment><@_u page="/submission/${assignment.id}/request-access"/></#macro>
<#macro assignmentdelete assignment><@_u page="/admin/assignments/${assignment.id}/delete"/></#macro>
<#macro create_sitsassignments department academicYear="">
	<#if academicYear?has_content>
		<@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/setup-assignments" />
	<#else>
		<@_u page="/admin/department/${department.code}/setup-assignments" />
	</#if>
</#macro>
<#macro assignmentSharedOptions department><@_u page="/admin/department/${department.code}/shared-options" /></#macro>

<#macro copy_assignments_previous department academicYear="">
	<#if academicYear?has_content>
		<@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/copy-assignments" />
	<#else>
		<@_u page="/admin/department/${department.code}/copy-assignments" />
	</#if>
</#macro>
<#macro copy_assignments_previous_module module academicYear><@_u page="/admin/${module.code}/${academicYear.startYear?c}/copy-assignments" /></#macro>

<#macro editassignmentdetails assignment><@_u page="/admin/assignments/${assignment.id}/edit" /></#macro>

<#macro feedbackSummary assignment studentid><@_u page="/admin/assignments/${assignment.id}/feedback/summary/${studentid}"/></#macro>
<#macro feedbackAudit assignment studentid><@_u page="/admin/assignments/${assignment.id}/audit/${studentid}"/></#macro>
<#macro plagiarismInvestigation assignment><@_u page='/admin/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised'/></#macro>
<#macro onlinefeedback assignment><@_u page="/admin/assignments/${assignment.id}/feedback/online"/></#macro>
<#macro onlinefeedbackform assignment student><@_u page="/admin/assignments/${assignment.id}/feedback/online/${student.userId}"/></#macro>
<#macro feedbackAdjustment assignment><@_u page="/admin/assignments/${assignment.id}/feedback/adjustments"/></#macro>

<#macro feedbacktemplates department><@_u page="/admin/department/${department.code}/settings/feedback-templates" /></#macro>
<#macro editfeedbacktemplate department template><@_u page="/admin/department/${department.code}/settings/feedback-templates/edit/${template.id}" /></#macro>
<#macro deletefeedbacktemplate department template><@_u page="/admin/department/${department.code}/settings/feedback-templates/delete/${template.id}" /></#macro>
<#macro feedbacktemplatedownload department feedbacktemplate><@_u page="/admin/department/${department.code}/settings/feedback-templates/download/${feedbacktemplate.id}/${feedbacktemplate.attachment.name}" /></#macro>
<#macro assignment_in_profile assignment student><@_u page="/submission/${assignment.id}/${student.universityId}"/></#macro>

<#-- non admin -->
<#macro assignment assignment><@_u page="/submission/${assignment.id}"/></#macro>
<#macro submission_attempt assignment><@_u page="/submission/${assignment.id}/attempt" /></#macro>
<#macro extensionRequest assignment><@_u page="/assignment/${assignment.id}/extension"/></#macro>
<#macro extensionRequestAttachment assignment attachment><@_u page="/assignment/${assignment.id}/extension/supporting-file/${attachment.name?url}"/></#macro>
<#macro assignmentreceipt assignment><@_u page="/submission/${assignment.id}/resend-receipt"/></#macro>
<#macro submissionReceiptPdf submission><@_u page="/submission/${submission.assignment.id}/submission-receipt.pdf"/></#macro>
<#macro submissionReceiptPdf_in_profile assignment><@_u page="submission/${submission.assignment.id}/${submission.usercode}/submission-receipt.pdf"/></#macro>
<#macro submissionAttachment submission attachment><@_u page="/submission/${submission.assignment.id}/attachment/${attachment.name?url}" /></#macro>
<#macro submissionAttachment_in_profile submission attachment><@_u page="submission/${submission.assignment.id}/${submission.usercode}/attachment/${attachment.name?url}" /></#macro>
<#macro feedbackPdf assignment feedback><@_u page="/submission/${assignment.id}/${feedback.usercode}/feedback.pdf"/></#macro>
<#macro feedbackAttachment feedback attachment><@_u page="/submission/${feedback.assignment.id}/get/${attachment.name?url}"/></#macro>
<#macro feedbackAttachment_in_profile feedback attachment><@_u page="/submission/${feedback.assignment.id}/${feedback.universityId}/get/${attachment.name?url}"/></#macro>

<#macro assignemnts_json module><@_u page="/admin/${module.code}/assignments" /></#macro>
<#macro enrolment assignment><@_u page="/admin/assignments/${assignment.id}/enrolment"/></#macro>

<#macro submitToTurnitin assignment><@_u page="/admin/assignments/${assignment.id}/turnitin"/></#macro>
<#macro turnitinLtiReport assignment attachment><@_u page="/admin/assignments/${assignment.id}/turnitin/lti-report/${attachment.id}"/></#macro>
<#macro turnitinReport assignment attachment><@_u page="/admin/assignments/${assignment.id}/turnitin/report/${attachment.id}"/></#macro>

<#macro listmarkersubmissions assignment marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}"/></#macro>

<#macro markerUploadFeedback assignment marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/feedback"/></#macro>
<#macro markerUploadMarks assignment marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/marks"/></#macro>
<#macro markerUploadMarksTemplate assignment marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/marks-template"/></#macro>

<#macro markerOnlineFeedback assignment stage marker student><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/${stage.name}/feedback/online/${student.userId}"/></#macro>
<#macro markingCompleted assignment stageOrder marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/${stageOrder}/marking-completed"/></#macro>
<#macro finishMarking assignment stageOrder marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/${stageOrder}/finish-marking"/></#macro>

<#macro downloadMarkerFeedbackOne assignment marker markerFeedback attachment><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/feedback/download/${markerFeedback.id}/attachment/${attachment.name?url}"/></#macro>
<#macro downloadMarkerFeedbackAll assignment marker markerFeedback zipName><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/feedback/download/${markerFeedback.id}/attachments/${zipName}.zip"/></#macro>
<#macro downloadMarkerFeedbackStage assignment marker stage><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/${stage.name}/feedback.zip" /></#macro>
<#macro downloadMarkerSubmissions assignment marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/submissions.zip"/></#macro>
<#macro downloadMarkerSubmissionsPdf assignment marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/submissions.pdf"/></#macro>

<#macro markerTemplatesZip assignment marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/marker-templates.zip" /></#macro>

<#macro assignmentsubmissionsandfeedback assignment><@_u page="/admin/assignments/${assignment.id}"/></#macro>
<#macro assignmentsubmissionsandfeedbacktable assignment><@_u page="/admin/assignments/${assignment.id}/table"/></#macro>
<#macro assignmentsubmissionsandfeedbacksummary assignment><@_u page="/admin/assignments/${assignment.id}/summary"/></#macro>
<#macro releaseForMarking assignment><@_u page="/admin/assignments/${assignment.id}/release-submissions"/></#macro>
<#macro returnToMarker assignment><@_u page="/admin/assignments/${assignment.id}/return-submissions"/></#macro>
<#macro stopMarking assignment><@_u page="/admin/assignments/${assignment.id}/stop-marking"/></#macro>
<#macro moderationSampling assignment><@_u page="/admin/assignments/${assignment.id}/moderator-sampling" /></#macro>
<#macro moderationSamplingAllocation assignment><@_u page="/admin/assignments/${assignment.id}/moderator-sampling/allocate" /></#macro>
<#macro moderationSamplingFinalise assignment><@_u page="/admin/assignments/${assignment.id}/moderator-sampling/finalise-feedback" /></#macro>

<#macro assignmentextensions assignment><@_u page="/admin/assignments/${assignment.id}/extensions"/></#macro>

<#macro genericfeedback assignment><@_u page="/admin/assignments/${assignment.id}/feedback/generic"/></#macro>
<#macro uploadToSits assignment><@_u page="/admin/assignments/${assignment.id}/upload-to-sits"/></#macro>
<#macro checkSitsUpload feedback><@_u page="/admin/assignments/${feedback.assignment.id}/feedback/${feedback.id}/check-sits"/></#macro>

<#macro manageMarksClosure><@_u page="/admin/marksmanagement/departments" /></#macro>

<#macro submitToTurnitin assignment><@_u page="/admin/assignments/${assignment.id}/turnitin"/></#macro>
<#macro submitToTurnitinStatus assignment><@_u page="/admin/assignments/${assignment.id}/turnitin/status"/></#macro>

<#macro feedbackBulkAdjustment assignment><@_u page="/admin/assignments/${assignment.id}/feedback/bulk-adjustment"/></#macro>
<#macro feedbackBulkAdjustmentTemplate assignment><@_u page="/admin/assignments/${assignment.id}/feedback/bulk-adjustment/template"/></#macro>
<#macro feedbackAdjustmentForm assignment studentid><@_u page="/admin/assignments/${assignment.id}/feedback/adjustments/${studentid}"/></#macro>
<#macro generateGradesForMarks assignment><@_u page="/admin/assignments/${assignment.id}/generate-grade"/></#macro>

<#macro assignmentSubmissionSummary assignment><@_u page="/admin/assignments/${assignment.id}/summary"/></#macro>
<#macro assignmentSubmissionTable assignment><@_u page="/admin/assignments/${assignment.id}/table"/></#macro>

<#macro submissionsZip assignment><@_u page="/admin/assignments/${assignment.id}/submissions.zip" /></#macro>
<#macro submissionsPdf assignment><@_u page="/admin/assignments/${assignment.id}/submissions.pdf" /></#macro>
<#macro assignmentFeedbackZip assignment><@_u page="/admin/assignments/${assignment.id}/feedback.zip" /></#macro>

<#macro deleteSubmissionsAndFeedback assignment><@_u page="/admin/assignments/${assignment.id}/submissionsandfeedback/delete" /></#macro>

<#macro downloadFeedbackTemplates assignment><@_u page="/admin/assignments/${assignment.id}/feedback-templates.zip" /></#macro>
<#macro publishFeedback assignment><@_u page="/admin/assignments/${assignment.id}/publish" /></#macro>
<#macro exportCsv assignment><@_u page="/admin/assignments/${assignment.id}/export.csv" /></#macro>
<#macro exportXml assignment><@_u page="/admin/assignments/${assignment.id}/export.xml" /></#macro>
<#macro exportXlsx assignment><@_u page="/admin/assignments/${assignment.id}/export.xlsx" /></#macro>
<#macro addMarks assignment><@_u page="/admin/assignments/${assignment.id}/marks" /></#macro>
<#macro addFeedback assignment><@_u page="/admin/assignments/${assignment.id}/feedback/batch" /></#macro>

<#macro feedbackDownload feedback attachmentExtension><@_u page="/admin/assignments/${feedback.assignment.id}/feedback/download/${feedback.id}/feedback-${feedback.studentIdentifier}.${attachmentExtension}" /></#macro>

<#macro uploadmarkerfeedback assignment marker><@_u page="/admin/assignments/${assignment.id}/marker/${marker.userId}/feedback"/></#macro>
<#macro markerFeedbackFilesDownload markerFeedback><@_u page="/admin/assignments/${markerFeedback.feedback.assignment.id}/marker/${markerFeedback.marker.userId}/feedback/download/${markerFeedback.id}/" /></#macro>

<#macro feedbackZip feedback><@_u page="/submission/${feedback.assignment.id}/all/feedback.zip" /></#macro>
<#macro feedbackZip_in_profile feedback><@_u page="/submission/${feedback.assignment.id}/${feedback.universityId}/all/feedback.zip" /></#macro>
