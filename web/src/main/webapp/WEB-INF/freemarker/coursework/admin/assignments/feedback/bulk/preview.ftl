<#escape x as x?html>
<h1>Preview bulk adjustment</h1>

<div class="fix-area">
	<#if command.ignoredRows?has_content>
		<details>
			<summary>
				There were some rows that either had no university ID, were for an invalid user, or were for a student without feedback on this assignment.</br>
				They will be ignored.
			</summary>

			<table class="table table-bordered table-striped table-condensed">
				<tbody>
					<#list command.ignoredRows as row>
						<tr>
							<#list row?keys as key>
								<td>${row[key]}</td>
							</#list>
						</tr>
					</#list>
				</tbody>
			</table>
		</details>
		<p>

		</p>
	</#if>

	<#assign formUrl><@routes.coursework.feedbackBulkAdjustment assignment /></#assign>
	<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="command" class="form-horizontal">
		<#list command.students as usercode>
			<input type="hidden" name="students" value="${usercode}" />
		</#list>
		<#list command.marks?keys as usercode>
			<input type="hidden" name="marks[${usercode}]" value="${command.marks[usercode]!""}" />
		</#list>
		<#list command.grades?keys as usercode>
			<input type="hidden" name="grades[${usercode}]" value="${command.grades[usercode]!""}" />
		</#list>
		<#list command.reasons?keys as usercode>
			<input type="hidden" name="reasons[${usercode}]" value="${command.reasons[usercode]!""}" />
		</#list>
		<#list command.comments?keys as usercode>
			<input type="hidden" name="comments[${usercode}]" value="${command.comments[usercode]!""}" />
		</#list>

		<@form.labelled_row "privateAdjustment" "Hide from student">
			<@f.checkbox path="privateAdjustment" />
			<@fmt.help_popover id="privateAdjustmentHelp" title="Hide from student" content="If checked these adjustments will not be visible to students" />
		</@form.labelled_row>

		<#if command.requiresDefaultReason || command.requiresDefaultComments>
			<p>Some of the adjustments do not have a reason or comment. You need to provide them:</p>

			<@form.labelled_row "defaultReason" "Reason">
				<@f.input path="defaultReason" />
			</@form.labelled_row>
			<@form.labelled_row "defaultComment" "Comment">
				<@f.textarea path="defaultComment" cssClass="big-textarea" />
			</@form.labelled_row>
		</#if>

		<@spring.bind path="command">
			<#assign hasErrors=status.errors.allErrors?size gt 0 />
		</@spring.bind>

		<#if hasErrors>
			<div class="alert alert-warning">
				Some of the rows have errors, which are shown below. These rows will be ignored.
			</div>
		</#if>

		<table class="table table-bordered table-condensed">
			<thead>
				<tr>
					<th>University ID</th>
					<th>Adjusted mark</th>
					<th>Adjusted grade</th>
					<th>Reason</th>
					<th>Comments</th>
					<#if hasErrors><th>Errors</th></#if>
				</tr>
			</thead>
			<tbody>
				<#list command.students as universityId>
					<@spring.bind path="command.marks[${universityId}]">
						<#assign markErrors=status.errorMessages />
					</@spring.bind>
					<@spring.bind path="command.grades[${universityId}]">
						<#assign gradeErrors=status.errorMessages />
					</@spring.bind>
					<@spring.bind path="command.reasons[${universityId}]">
						<#assign reasonErrors=status.errorMessages />
					</@spring.bind>
					<#assign allErrors = markErrors + gradeErrors + reasonErrors />
					<tr <#if allErrors?has_content>class="alert-error"</#if>>
						<td>${universityId}</td>
						<td>${command.marks[universityId]!""}</td>
						<td>${command.grades[universityId]!""}</td>
						<td>${command.reasons[universityId]!""}</td>
						<td>${command.comments[universityId]!""}</td>
						<#if hasErrors>
							<td>
								<#list allErrors as error>
									${error}<#if error_has_next><br/></#if>
								</#list>
							</td>
						</#if>
					</tr>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons fix-footer">
			<input type="hidden" name="confirmStep" value="true">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class="btn" href="<@routes.coursework.assignmentsubmissionsandfeedbacksummary assignment />">Cancel</a>
		</div>

	</@f.form>
</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
	})
</script>
</#escape>