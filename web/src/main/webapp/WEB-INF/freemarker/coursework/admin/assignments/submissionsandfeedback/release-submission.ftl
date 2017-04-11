<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#assign formAction><@url page='/coursework/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/release-submissions'/></#assign>

<@f.form method="post" action="${formAction}" commandName="releaseForMarkingCommand">

	<h1>Release submissions to markers for ${assignment.name}</h1>

	<@form.errors path="" />
	<input type="hidden" name="confirmScreen" value="true" />

	<#if releaseForMarkingCommand.unreleasableSubmissions?has_content>
		<p>
			<a href="" class="invalid-submissions"><@fmt.p (releaseForMarkingCommand.unreleasableSubmissions?size ) "submission" /></a>
			could not be released for marking.
		</p>
		<div class="hidden invalid-submissions-list">
			<#if releaseForMarkingCommand.studentsAlreadyReleased?has_content>
				<p>Already released for marking</p>
				<ul><#list releaseForMarkingCommand.studentsAlreadyReleased as submission>
					<li>${submission}</li>
				</#list></ul>
			</#if>
			<#if releaseForMarkingCommand.studentsWithoutKnownMarkers?has_content>
				<p>No marker allocated</p>
				<ul><#list releaseForMarkingCommand.studentsWithoutKnownMarkers as submission>
					<li>${submission}</li>
				</#list></ul>
			</#if>
		</div>
		<script>
			var listHtml = jQuery(".invalid-submissions-list").html();
			jQuery(".invalid-submissions").on('click',function(e){e.preventDefault()})
			jQuery(".invalid-submissions").tooltip({
				html: true,
				placement: 'right',
				title: listHtml
			});
		</script>
	</#if>

	<#if releaseForMarkingCommand.students?size - releaseForMarkingCommand.unreleasableSubmissions?size != 0>

		<@spring.bind path="students">
			<@form.errors path="students" />
			<#assign students=status.actualValue />
			<p>
				Releasing <strong><@fmt.p (students?size - releaseForMarkingCommand.unreleasableSubmissions?size ) "student" /></strong> submissions to markers.
			</p>
			<#list students as usercode>
				<input type="hidden" name="students" value="${usercode}" />
			</#list>
		</@spring.bind>

		<p>
			<@form.errors path="confirm" />
			<@form.label checkbox=true><@f.checkbox path="confirm" />
				<#if (students?size > 1)>
					I confirm that I want to release these students' submissions to markers.
				<#else>
					I confirm that I want to release this student's submission to the marker.
				</#if>
			</@form.label>
		</p>

		<div class="submit-buttons">
			<input class="btn btn-warn" type="submit" value="Confirm">
			<a class="btn" href="<@routes.coursework.assignmentsubmissionsandfeedback assignment />">Cancel</a>
		</div>

	<#else>
		<div class="alert alert-warning">
			None of the students that you selected can be released for marking. <a href="<@routes.coursework.assignmentsubmissionsandfeedback assignment />">Go back to the assignment summary.</a>
		</div>
	</#if>

</@f.form>
</#escape>