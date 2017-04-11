<#escape x as x?html>
<#compress>
	<#assign time_remaining=durationFormatter(assignment.closeDate) />
	<h1>Request an extension</h1>
	<h4><span class="muted">for</span> ${assignment.name}</h4>
	<#if !assignment.newExtensionsCanBeRequested>
		<p>
			This assignment closed <@fmt.date date=assignment.closeDate /> (${time_remaining}).
			You cannot request an extension after the close date has passed.
		</p>
	<#else>

		<#if isModification>
			<#if existingRequest.approved>
				<div class="alert alert-success">
					<#assign approved_ago=durationFormatter(existingRequest.reviewedOn) />
					Your extension request was approved - <@fmt.date date=existingRequest.reviewedOn /> (${approved_ago}).
				</div>
			<#elseif existingRequest.rejected>
				<div class="alert alert-error">
					Your extension request has been rejected.
				</div>
			<#else>
				<div class="alert alert-info">Your extension request is being reviewed.</div>
			</#if>
			<#if existingRequest.expiryDate?? || existingRequest.reviewerComments??>
				<div class="form-horizontal">
					<#if existingRequest.expiryDate?? && existingRequest.approved>
						<div class="control-group">
							<label class="control-label">New submission deadline</label>
							<div class="controls">
								<p>
									Your new submission deadline is <@fmt.date date=existingRequest.expiryDate at=true/>
								</p>
							</div>
						</div>
					</#if>
					<#if existingRequest.reviewerComments??>
						<div class="control-group">
							<label class="control-label">Review comments</label>
							<div class="controls"><p>${existingRequest.reviewerComments}</p></div>
						</div>
					</#if>
				</div>
				<hr/>
			</#if>
			<#assign time_since_request=durationFormatter(existingRequest.requestedOn) />
			<p>
				You requested an extension for this assignment <@fmt.date date=existingRequest.requestedOn /> (${time_since_request}).
				Use the form below to update the details of your extension request. If you have been asked to provide
				additional information about your request or for supporting documentation you can add them here.
			</p>
		<#else>
			<p>
				This assignment closes <@fmt.date date=assignment.closeDate /> (${time_remaining} remaining).
				To request an extension for this assignment please read the Extension Guidelines below and submit this form.
				You will receive a notification when your request has been processed.
			</p>
			<#if department.extensionGuidelineSummary??>
				<#include "/WEB-INF/freemarker/coursework/submit/formfields/guideline.ftl" >
			</#if>
			<#if department.extensionGuidelineLink?has_content>
				<p>You should read the full <a href="${department.extensionGuidelineLink}">extension guidelines</a>
				before submitting your application for an extension.</p>
			</#if>
		</#if>

		<@f.form method="post" enctype="multipart/form-data" class="form-horizontal double-submit-protection extension-request" action="${url('/coursework/module/${module.code}/${assignment.id}/extension')}" commandName="command">

			<#if isModification>
				<@f.hidden path="modified" value="true" />
			</#if>

			<@form.labelled_row "" "Your University ID">
				<div class="uneditable-input">${user.studentIdentifier}</div>
			</@form.labelled_row>

			<@form.labelled_row "reason" "Please give a full statement of your reasons for applying for an extension">
				<@f.textarea path="reason" cssClass="text big-textarea" maxlength=4000/>
			</@form.labelled_row>

			<#if features.disabilityRenderingInExtensions && (profile.disability.reportable)!false>
				<@form.labelled_row "disabilityAdjustment" "Report your disability">
					<label class="checkbox">
						<@f.checkbox path="disabilityAdjustment" /> I wish to disclose my ${profile.disability.definition} to the reviewer as part of this request.
					</label>
				</@form.labelled_row>
			</#if>

			<span id="assignmentCloseDate" data-close-date="${assignment.closeDate}" />
			<@form.labelled_row "requestedExpiryDate" "Requested extension date">
				<@f.input path="requestedExpiryDate" cssClass="date-time-picker" />
			</@form.labelled_row>

			<@form.filewidget
				basename="file"
				labelText="Upload supporting documentation relevant to your request"
				types=[]
				multiple=true
				required=assignment.extensionAttachmentMandatory
			/>

			<#if command.attachedFiles?has_content >
				<@form.labelled_row "attachedFiles" "Supporting documents">
				<ul>
					<#list command.attachedFiles as attachment>
						<li id="attachment-${attachment.id}" class="attachment">
							<a href="<@routes.coursework.extensionrequestattachment assignment=assignment filename=attachment.name />"><#compress>
								${attachment.name}
							</#compress></a>&nbsp;
							<@f.hidden path="attachedFiles" value="${attachment.id}" />
							<a class="remove-attachment" href="">Remove</a>
						</li>
					</#list>
				</ul>
				<script>
					jQuery(function($){
						$(".remove-attachment").on("click", function(e){
							e.preventDefault();
							$(this).closest("li.attachment").remove();
						});
					});
				</script>
				<div class="help-block">
					This is a list of all supporting documents that have been attached to this extension request. Click the
					remove link next to a document to delete it.
				</div>
				</@form.labelled_row>
			</#if>

			<@form.row>
				<@form.label></@form.label>
				<@form.field>
					<label class="checkbox">
						<@f.checkbox path="readGuidelines" />
						I confirm that I have read the extension guidelines.
						</label>
						<@f.errors path="readGuidelines" cssClass="error" />
				</@form.field>
			</@form.row>

			<input type="hidden" name="returnTo" value="${returnTo}" />

			<div class="submit-buttons form-actions">
				<input type="submit" class="btn btn-primary" value="Submit" />
				<a class="btn" href="${returnTo}">Cancel</a>
			</div>
		</@f.form>

	<script>
		(function($) {
			$form = $(".extension-request");
			var $expiryDateField = $form.find('.date-time-picker');
			$expiryDateField.tabulaDateTimePicker();
			var expiryDatePicker = $expiryDateField.data('datetimepicker');
			var closeDate = new Date($form.find('#assignmentCloseDate').data("close-date"));
			if (closeDate) {
				expiryDatePicker.setStartDate(closeDate);
			}
		})(jQuery);
	</script>
	</#if>
</#compress>
</#escape>