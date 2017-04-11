<#escape x as x?html>

<h1>Add students manually</h1>

<form action="" method="POST" class="mass-add-users">

	<input type="hidden" name="filterQueryString" value="${findCommand.filterQueryString!""}">
	<@f.hidden path="findCommand.linkToSits" />

	<#list findCommand.staticStudentIds as id>
		<input type="hidden" name="staticStudentIds" value="${id}" />
	</#list>
	<#list editMembershipCommand.includedStudentIds as id>
		<input type="hidden" name="includedStudentIds" value="${id}" />
	</#list>
	<#list editMembershipCommand.excludedStudentIds as id>
		<input type="hidden" name="excludedStudentIds" value="${id}" />
	</#list>
	<input type="hidden" name="returnTo" value="${returnTo}">

	<p>Type or paste in a list of usercodes or University numbers here, separated by white space, then click <code>Add</code>.</p>

	<textarea rows="6" class="form-control" name="massAddUsers"></textarea>

	<input
		type="submit"
		class="btn btn-primary spinnable spinner-auto add-students"
		name="${ManageDepartmentSmallGroupsMappingParameters.manuallyAddSubmit}"
		value="Add"
	/>

</form>

</#escape>