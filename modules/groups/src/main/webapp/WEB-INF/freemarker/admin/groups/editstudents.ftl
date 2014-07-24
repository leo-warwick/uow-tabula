<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Edit small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<@f.form id="editStudents" method="POST" commandName="command" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right">Properties</span>
			<span class="arrow-right arrow-left active">Students</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and edit groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddGroups}">Groups</button></span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and edit events"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddEvents}">Events</button></span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAllocate}">Allocate</button></span>
		</p>

		<#include "_editStudents.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageSmallGroupsMappingParameters.editAndAddGroups}"
				value="Save and add groups"
				title="Add groups to this set"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save and exit"
				title="Save and add groups later"
				data-container="body"
				/>
			<a class="btn" href="<@routes.depthome module=smallGroupSet.module />">Cancel</a>
		</div>
	</@f.form>
</#escape>