<#escape x as x?html>
<#compress>
	<div class="deptheader">
		<h1>Request an extension<#if isModification && existingRequest.moreInfoRequired> - More information required</#if></h1>
		<h5 class="with-related">for ${assignment.module.code?upper_case} - ${assignment.name}</h5>
	</div>

	<#assign time_remaining=durationFormatter(assignment.closeDate) />
	<#if !assignment.newExtensionsCanBeRequested>
		<p>
			This assignment closed <@fmt.date date=assignment.closeDate /> (${time_remaining}).
			You cannot request an extension after the close date has passed.
		</p>
	<#else>
		<#if isModification>
			<#if existingRequest.approved>
				<div class="alert alert-info">
					<#assign approved_ago=durationFormatter(existingRequest.reviewedOn) />
					Your extension request was approved - <@fmt.date date=existingRequest.reviewedOn /> (${approved_ago}).
				</div>
			<#elseif existingRequest.rejected>
				<div class="alert alert-danger">
					Your extension request has been rejected.
				</div>
			<#elseif existingRequest.moreInfoRequired>
				<div class="alert alert-info">
					<div class="control-group">
						<label class="control-label">Request for more information</label>
						<div class="controls"><p>${existingRequest.reviewerComments}</p></div>
					</div>
				</div>
			<#else>
				<div class="alert alert-info">Your extension request is being reviewed.</div>
			</#if>
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
			<#if existingRequest.reviewerComments?? && !existingRequest.moreInfoRequired>
				<div class="control-group">
					<label class="control-label">Review comments</label>
					<div class="controls"><p>${existingRequest.reviewerComments}</p></div>
				</div>
			</#if>
			<#if existingRequest.expiryDate?? || existingRequest.reviewerComments??>
				<hr/>
			</#if>
			<#assign time_since_request=durationFormatter(existingRequest.requestedOn) />
			<p>
				You requested an extension for this assignment <@fmt.date date=existingRequest.requestedOn /> (${time_since_request}).
				Use the form below to update the details of your extension request.
			</p>
		<#else>
			<p>
				This assignment closes <@fmt.date date=assignment.closeDate /> (${time_remaining} remaining).
				To request an extension for this assignment please read the Extension Guidelines below and submit this form.
				You will receive a notification when your request has been processed.
			</p>
			<div id="extensionGuidelines">
				<#if department.extensionGuidelineSummary??>
					<#include "/WEB-INF/freemarker/cm2/submit/formfields/guideline.ftl" >
				</#if>
				<#if department.extensionGuidelineLink?has_content>
					<p>
						You should read the full <a href="${department.extensionGuidelineLink}">extension guidelines</a>
						before submitting your application for an extension.
					</p>
				</#if>
			</div>
		</#if>

		<#assign formAction><@routes.cm2.extensionRequest assignment /></#assign>

		<@f.form
			method="post"
			enctype="multipart/form-data"
			class="double-submit-protection"
			action="${formAction}"
			commandName="command"
		>

			<@bs3form.labelled_form_group "" "Your University ID">
				<div class="uneditable-input">${user.studentIdentifier}</div>
			</@bs3form.labelled_form_group>

			<@bs3form.labelled_form_group "reason" "Reasons for applying for an extension">
				<@f.textarea path="reason" cssClass="form-control text big-textarea" maxlength=4000/>
			</@bs3form.labelled_form_group>

			<#if features.disabilityRenderingInExtensions && (profile.disability.reportable)!false>
				<@bs3form.checkbox path="disabilityAdjustment">
					<@f.checkbox path="disabilityAdjustment"/> I wish to disclose my ${profile.disability.definition} to the reviewer as part of this request.
				</@bs3form.checkbox>
			</#if>

			<span id="assignmentCloseDate" data-close-date="${assignment.closeDate}" />

			<@bs3form.labelled_form_group "requestedExpiryDate" "Requested extension date">
				<div class="input-group">
					<@f.input path="requestedExpiryDate" cssClass="form-control date-time-picker" />
					<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
				</div>
			</@bs3form.labelled_form_group>

			<@bs3form.filewidget
				basename="file"
				labelText="Upload supporting documentation relevant to your request"
				types=[]
				multiple=true
				required=assignment.extensionAttachmentMandatory
			/>

			<#if command.attachedFiles?has_content >
				<@bs3form.labelled_form_group path="attachedFiles" labelText="Attached files">
					<ul  class="unstyled">
						<#list command.attachedFiles as attachment>
							<#assign url></#assign>
							<li id="attachment-${attachment.id}" class="attachment">
								<i class="fa fa-file-o"></i>
								<a href="<@routes.cm2.extensionRequestAttachment assignment attachment />"><#compress>
									${attachment.name}
								</#compress></a>&nbsp;
								<@f.hidden path="attachedFiles" value="${attachment.id}" />
								<i class="fa fa-times-circle remove-attachment"></i>
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
						This is a list of all supporting documents that have been attached to this extension request.
						Click the remove link next to a document to delete it.
					</div>
				</@bs3form.labelled_form_group>
			</#if>

			<@bs3form.checkbox path="readGuidelines">
				<@f.checkbox path="readGuidelines"/> I confirm that I have read the <a href="#extensionGuidelines">extension guidelines</a>.
			</@bs3form.checkbox>

			<input type="hidden" name="returnTo" value="${returnTo}" />

			<div class="submit-buttons form-actions">
			<#if isModification && existingRequest.moreInfoRequired>
				<input type="submit" class="btn btn-primary" value="Send reply" />
			<#else>
				<input type="submit" class="btn btn-primary" value="Submit" />
			</#if>
				<a class="btn" href="${returnTo}">Cancel</a>
			</div>
		</@f.form>
	</#if>
</#compress>
</#escape>