<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign commandName="addMarkerFeedbackCommand" />

<@spring.bind path=commandName>
<#assign hasErrors=status.errors.allErrors?size gt 0 />
<#assign hasGlobalErrors=status.errors.globalErrors?size gt 0 />
</@spring.bind>

<@f.form method="post" class="double-submit-protection"  action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/marker/${marker.warwickId}/feedback')}" commandName=commandName>
<input type="hidden" name="batch" value="true">

<h1>Submit feedback for ${assignment.name}</h1>

<@spring.bind path="fromArchive"><#assign fromArchive=status.actualValue /></@spring.bind>
<#if fromArchive>
<#assign verbed_your_noun="unpacked your Zip file"/>
<#else>
<#assign verbed_your_noun="received your files"/>
</#if>

<@spring.bind path="items">
<#assign itemsList=status.actualValue />
<p>
	<#if itemsList?size gt 0>
		I've ${verbed_your_noun} and I found feedback for <@fmt.p number=itemsList?size singular="student" plural="students" shownumber=true />
		<#if hasErrors>
			<div class="alert alert-error">
				However, there were some problems with its contents, which are shown below.
				You'll need to correct these problems with the zip and try again.
			</div>
		</#if>
	<#else>
		<div class="alert alert-error">
			I've ${verbed_your_noun} but I couldn't find any files that looked like feedback items.
		</div>
	</#if>

</p>
</@spring.bind>

<#if hasGlobalErrors>
	<div class="alert alert-error"><@f.errors path="" cssClass="error"/></div>
<#else>

<#if addMarkerFeedbackCommand.unrecognisedFiles?size gt 0>
<div class="unrecognised-files alert alert-block">
<div>I didn't understand some of the files uploaded, and these will be ignored:</div>
<ul class="file-list">
<#list addMarkerFeedbackCommand.unrecognisedFiles as unrecognisedFile>
<li>
	<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}].path" />
	<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}].file" />
	${unrecognisedFile.path}
</li>
</#list>
</ul>
</div>
</#if>

<#if addMarkerFeedbackCommand.moduleMismatchFiles?size gt 0>
<div class="invalid-files alert alert-error">
<div>There were some files with problem names, which look as if they may belong to another module. Please check these before confirming.</div>
<ul class="file-list">
<#list addMarkerFeedbackCommand.moduleMismatchFiles as moduleMismatchFile>
<li>
	<@f.hidden path="moduleMismatchFiles[${moduleMismatchFile_index}].path" />
	<@f.hidden path="moduleMismatchFiles[${moduleMismatchFile_index}].file" />
	${moduleMismatchFile.path}
</li>
</#list>
</ul>
</div>
</#if>

<#if addMarkerFeedbackCommand.invalidFiles?size gt 0>
<div class="invalid-files alert alert-block alert-error">
<div>There were some files with problem names. You'll need to fix these and then try uploading again.</div>
<ul class="file-list">
<#list addMarkerFeedbackCommand.invalidFiles as invalidFile>
<li>
	<@f.hidden path="invalidFiles[${invalidFile_index}].path" />
	<@f.hidden path="invalidFiles[${invalidFile_index}].file" />
	${invalidFile.path}
</li>
</#list>
</ul>
</div>
</#if>

<#if addMarkerFeedbackCommand.invalidStudents?size gt 0>
<div class="invalid-students alert">
<div>Some of the feedback that you uploaded was for students that you are not assigned to mark. These files will be ignored</div>
<ul class="file-list">
	<#list addMarkerFeedbackCommand.invalidStudents as invalidStudent>
	<li>
		${invalidStudent.uniNumber}
	</li>
</#list>
</ul>
</div>
</#if>

<#if addMarkerFeedbackCommand.markedStudents?size gt 0>
<div class="invalid-students alert">
<div>Some of the feedback that you uploaded was for students that you have finished marking. These files will be ignored</div>
<ul class="file-list">
	<#list addMarkerFeedbackCommand.markedStudents as markedStudent>
	<li>
		${markedStudent.uniNumber}
	</li>
</#list>
</ul>
</div>
</#if>


<@spring.bind path="items">
<#assign itemList=status.actualValue />
<#if itemList?size gt 0>
<table class="table table-bordered table-striped">
	<tr>
		<th>University ID</th>
		<th>Files</th>
	</tr>
<#list itemList as item>
	<tr>
	<@spring.nestedPath path="items[${item_index}]">
		<@f.hidden path="uniNumber" />
		<td>
			<@spring.bind path="uniNumber">
				${status.value}
			</@spring.bind>
			<@f.errors path="uniNumber" cssClass="error" />
			<#if item.submissionExists>
				<span class="warning">Feedback already exists for this user. New files will be added to the existing ones</span>
			</#if>
		</td>
		<#noescape>
		<@spring.bind path="file.attached" htmlEscape=false>
		<td>
			<#-- FIXME should be able to spring:bind to a list, not have to manually specify it like this -->
			<ul class="file-list">
			<#list addMarkerFeedbackCommand.items[item_index].listAttachments as attached>
				<li>
					<@f.hidden path="file.attached[${attached_index}]" />
					${attached.name}
					<@f.errors path="file.attached[${attached_index}]" cssClass="error" />
					<#if attached.duplicate>
						<span class="warning">
							A feedback file with this name already exists for this student. It will be overwritten.
						</span>
					</#if>
				</li>
			</#list>
			</ul>
		</td>
		</@spring.bind>
		</#noescape>
	</@spring.nestedPath>
	</tr>
</#list>
</#if>
</@spring.bind>
</table>
</#if>

<div class="submit-buttons form-actions">
<#if hasErrors>
<input class="btn btn-primary" type="submit" value="Confirm" disabled="disabled">
<#else>
<input type="hidden" name="confirm" value="true">
<input class="btn btn-primary" type="submit" value="Confirm">
</#if>
	<a class="btn" href="<@routes.coursework.listmarkersubmissions assignment marker />">Cancel</a>
</div>
</@f.form>

</#escape>
