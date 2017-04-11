<#import "*/assignment_components.ftl" as components />
<#escape x as x?html>

<div class="deptheader">
	<h1>Create assignment</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area">
	<#assign actionUrl><@routes.cm2.createassignmentsubmissions assignment /></#assign>
	<@f.form method="post" action=actionUrl>
		<@components.assignment_wizard 'submissions' assignment.module false assignment/>
		<#include "_submissions_fields.ftl" />
		<div class="fix-footer">
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddOptions}"
					value="Save and continue"
			/>
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddSubmissions}"
					value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>