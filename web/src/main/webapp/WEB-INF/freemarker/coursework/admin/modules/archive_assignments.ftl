<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
	<h1>Archive assignments for ${title}</h1>
	<form action="" method="post" class="form-horizontal archive-assignments">
		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class='btn' href='<@url page=cancel />'>Cancel</a>
		</div>

		<#assign modules = archiveAssignmentsCommand.modules />
		<#assign path = "archiveAssignmentsCommand.assignments" />
		<#include "_assignment_list.ftl" />

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class='btn' href='<@url page=cancel />'>Cancel</a>
		</div>
	</form>
</#escape>