<#assign heading>
	<h2>Record a meeting</h2>
	<h6>
		<span class="muted">between tutor</span> ${tutorName}
		<span class="muted">and tutee</span> ${student.fullName}
	</h6>
</#assign>

<#if modal??>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		${heading}
	</div>
<#elseif iframe??>
	<div id="container">
<#else>
	${heading}
</#if>
<#if modal??>
	<div class="modal-body"></div>
	<div class="modal-footer">
		<form class="double-submit-protection">
			<span class="submit-buttons">
				<#assign title>Submit record for approval by personal <#if isStudent>tutor<#else>tutee</#if></#assign>
				<button title="${title}" class="btn btn-primary spinnable spinner-auto" type="submit" name="submit">
					Submit for approval
				</button>
				<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
			</span>
		</form>
	</div>
<#else>
	<@f.form id="meeting-record-form" method="post" enctype="multipart/form-data" action="${url('/tutor/meeting/' + student.universityId + '/create')}" commandName="createMeetingRecordCommand" class="form-horizontal double-submit-protection">
		<@form.labelled_row "title" "Title">
			<@f.input type="text" path="title" cssClass="input-block-level" maxlength="255" placeholder="Subject of meeting" />
		</@form.labelled_row>
		
		<#if allRelationships?size gt 1>
			<@form.labelled_row "relationship" "Tutor">
				<@f.select path="relationship" cssClass="input-large">
					<@f.option disabled="true" selected="true" label="Please select one..." />
					<@f.options items=allRelationships itemValue="agent" itemLabel="agentName" />
				</@f.select>
			</@form.labelled_row>
		</#if>

		<@form.labelled_row "meetingDate" "Date of meeting">
			<div class="input-append">
				<@f.input type="text" path="meetingDate" cssClass="input-medium date-picker" placeholder="Pick the date" />
				<span class="add-on"><i class="icon-calendar"></i></span>
			</div>
		</@form.labelled_row>

		<@form.labelled_row "format" "Format">
			<@f.select path="format" cssClass="input-large">
				<@f.option disabled="true" selected="true" label="Please select one..." />
				<@f.options items=formats />
			</@f.select>
		</@form.labelled_row>

		<#-- TODO: TinyMCE editor, bleh -->
		<@form.labelled_row "description" "Description (optional)">
			<@f.textarea rows="6" path="description" cssClass="input-block-level" />
		</@form.labelled_row>

		<#-- file upload (TAB-359) -->
		<#assign fileTypes=command.attachmentTypes />
		<@form.filewidget basename="file" types=fileTypes />

		<#if iframe??>
			<input type="hidden" name="modal" value="true" />
		<#else>
			<#-- separate page, not modal -->
			<div class="form-actions">
				<#assign title>Submit record for approval by personal <#if isStudent>tutor<#else>tutee</#if></#assign>
				<button title="${title}" class="btn btn-primary spinnable spinner-auto" type="submit" name="submit">
					Submit for approval
				</button>
				<a class="btn" href="<@routes.profile student />">Cancel</a>
			</div>
		</#if>
	</@f.form>
</#if>

<#if iframe??>
	</div> <#--container -->
</#if>

