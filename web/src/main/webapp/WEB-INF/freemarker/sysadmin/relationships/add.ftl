<#escape x as x?html>

<h1>Add a new student relationship type</h1>

<@f.form method="post" action="${url('/sysadmin/relationships/add')}" commandName="addStudentRelationshipTypeCommand">

	<@f.errors cssClass="error form-errors" />

	<#assign newRecord=true />
	<#include "_fields.ftl" />

	<@bs3form.form_group>
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn btn-default" href="<@url page="/sysadmin/relationships" />">Cancel</a>
	</@bs3form.form_group>

</@f.form>


</#escape>