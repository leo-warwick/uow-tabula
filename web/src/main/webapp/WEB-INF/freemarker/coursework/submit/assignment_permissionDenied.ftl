<#escape x as x?html>
	<#compress>
		<h1>${module.name} (${module.code?upper_case})
			<br><strong>${assignment.name}</strong></h1>

<#if can.do("Assignment.Update", assignment)>
	<h2>Assignment page for students</h2>

	<p>You can give students a link to this page to
		<#if assignment.collectSubmissions>submit work and to</#if>
		receive their feedback<#if assignment.collectMarks> and/or marks</#if>.</p>

	<p>If a student isn't in the enrolled list for your assignment, then they will receive a message on this page that
	   they're not enrolled, and they won't be able to submit until they've been added to the list.</p>

	<p><a class="btn" href="<@routes.coursework.depthome module/>">Module management - ${assignment.module.code}</a></p>
<#else>
	<#function has_admin module>
		<#list module.adminDepartment.owners.users as user>
			<#if user.foundUser && user.email?has_content>
				<#return true />
			</#if>
		</#list>

		<#return false />
	</#function>

		<#assign has_requested_access=RequestParameters.requestedAccess??/>
		<#if has_requested_access>
			<div class="alert alert-success">
				<a class="close" data-dismiss="alert">&times;</a>
				Thanks, we've sent a message to a department administrator with all the necessary
				details.
			</div>
		</#if>

		<h2>You're not enrolled</h2>

		<p>
			This assignment is set up only to allow students who are enrolled on the relevant module.
			If you are reading this and you believe you should have access to this assignment,
			<#if has_admin(module)>
				click the button below to send an automated message to an administrator for the department.
			<#else>
				contact your module convener or department.
			</#if>
		</p>

		<#if has_admin(module)>
			<#assign button_text>Request access for <strong>${user.fullName}</strong> (you)</#assign>

			<#if has_requested_access>
				<a href="#" class="btn disabled"><#noescape>${button_text}</#noescape></a>
			<#else>
				<form action="<@routes.coursework.assignmentrequestaccess assignment />" method="POST">
					<button class="btn" type="submit"><#noescape>${button_text}</#noescape></button>
				</form>
			</#if>
		</#if>
</#if>

	</#compress>
</#escape>