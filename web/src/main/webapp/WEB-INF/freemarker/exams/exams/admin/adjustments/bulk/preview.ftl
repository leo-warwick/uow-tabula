<#escape x as x?html>
<h1>Preview bulk adjustment</h1>

<div class="fix-area">
	<#if command.ignoredRows?has_content>
		<details>
			<summary>
				There were some rows that either had no university ID, were for an invalid user, or were for a student without feedback on this exam.</br>
				They will be ignored.
			</summary>

			<table class="table table-striped table-condensed">
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

	<#assign formUrl><@routes.exams.bulkAdjustment exam /></#assign>
	<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="command">
		<#list command.students as universityId>
			<input type="hidden" name="students" value="${universityId}" />
		</#list>
		<#list command.marks?keys as universityId>
			<input type="hidden" name="marks[${universityId}]" value="${command.marks[universityId]!""}" />
		</#list>
		<#list command.grades?keys as universityId>
			<input type="hidden" name="grades[${universityId}]" value="${command.grades[universityId]!""}" />
		</#list>
		<#list command.reasons?keys as universityId>
			<input type="hidden" name="reasons[${universityId}]" value="${command.reasons[universityId]!""}" />
		</#list>
		<#list command.comments?keys as universityId>
			<input type="hidden" name="comments[${universityId}]" value="${command.comments[universityId]!""}" />
		</#list>

		<@bs3form.checkbox>
			<@f.checkbox path="privateAdjustment" />
			Hide from student
			<@fmt.help_popover id="privateAdjustmentHelp" title="Hide from student" content="If checked these adjustments will not be visible to students" />
		</@bs3form.checkbox>

		<#if command.requiresDefaultReason || command.requiresDefaultComments>
			<p>Some of the adjustments do not have a reason or comment. You need to provide them:</p>

			<@bs3form.labelled_form_group path="defaultReason" labelText="Reason">
				<@f.input path="defaultReason" cssClass="form-control" />
			</@bs3form.labelled_form_group>
			<@bs3form.labelled_form_group path="defaultComment" labelText="Comment">
				<@f.textarea path="defaultComment" cssClass="form-control" />
			</@bs3form.labelled_form_group>
		</#if>

		<@spring.bind path="command">
			<#assign hasErrors=status.errors.allErrors?size gt 0 />
		</@spring.bind>

		<#if hasErrors>
			<div class="alert alert-danger">
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
					<tr <#if allErrors?has_content>class="danger"</#if>>
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
			<a class="btn btn-default" href="<@routes.exams.viewExam exam />">Cancel</a>
		</div>

	</@f.form>
</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
	})
</script>
</#escape>