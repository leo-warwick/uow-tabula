<#assign assignment = feedback.assignment />

<div class="modal-header">
	<a class="close" data-dismiss="modal" aria-hidden="true">&times;</a>
	<h6>Feedback Summary - ${feedback.studentIdentifier}</h6>
</div>
<div class="modal-body">
	<div class="form onlineFeedback">
		<form class="form-horizontal">


			<#if assignment.genericFeedback??>
				<div class="feedback-field">
					<div class="control-group">
						<label class="control-label">Generic feedback</label>
						<div class="controls">
							<p>${assignment.genericFeedback}</p>
						</div>
					</div>
				</div>
			</#if>

			<#if feedback.customFormValues?has_content>
				<div class="feedback-field">
					<div class="control-group">
						<label class="control-label">Feedback</label>
						<div class="controls">
						<#list feedback.customFormValues as formValue>
							<p>${formValue.value!""}</p>
						</#list>
						</div>
					</div>
				</div>
			</#if>

			<#if assignment.collectMarks>
				<div class="control-group">
					<label class="control-label">Mark</label>
					<div class="controls">
						<div class="input-append">
							<input type="text" disabled="disabled" value="${feedback.actualMark!""}" class="input-small">
							<span class="add-on">%</span>
						</div>
					</div>
				</div>

				<div class="control-group">
					<label class="control-label">Grade</label>
					<div class="controls">
						<input type="text" disabled="disabled" value="${feedback.actualGrade!""}" class="input-small">
					</div>
				</div>
			</#if>

			<#if feedback.attachments?has_content >
				<div class="control-group">
					<label class="control-label">Attachments</label>
					<div class="controls">

						<#if feedback.attachments?size == 1>
							<#assign attachmentExtension = feedback.attachments[0].fileExt>
						<#else>
							<#assign attachmentExtension = "zip">
						</#if>

						<a class="long-running" href="<@routes.coursework.adminFeedbackZip assignment feedback attachmentExtension />">
							<i class="icon-download"></i>
							<@fmt.p number=feedback.attachments?size singular="file" />
						</a>
					</div>
				</div>
			</#if>

		</form>
	</div>
</div>
