<#escape x as x?html>
	<#import "*/assignment_components.ftl" as components />

<div class="deptheader">
	<h1>Create a new assignment</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area">
	<#assign actionUrl><@routes.cm2.createassignmentfeedback assignment /></#assign>
	<@f.form method="post" action=actionUrl  cssClass="dirty-check">
		<@components.assignment_wizard 'feedback' assignment.module false module/>

		<@f.errors cssClass="error form-errors" />
		<#assign newRecord=false />
		<#include "_feedback_fields.ftl" />

		<div class="fix-footer">
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddStudents}"
					value="Save and continue"
			/>
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddFeedback}"
					value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>