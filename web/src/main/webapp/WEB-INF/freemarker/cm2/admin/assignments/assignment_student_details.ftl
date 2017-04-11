<#escape x as x?html>
<#import "*/assignment_components.ftl" as components />

<div class="deptheader">
	<h1>Create assignment</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area assignment-student-details">
	<#assign actionUrl><@routes.cm2.createassignmentstudents assignment /></#assign>
	<@f.form method="post" action=actionUrl>
		<@components.assignment_wizard 'students' assignment.module false assignment />
		<@f.errors cssClass="error form-errors" />
	<div>
		<#include "_student_fields.ftl" />
	</div>
		<div class="fix-footer">
			<input
				type="submit"
				class="btn btn-primary"
				name="${ManageAssignmentMappingParameters.createAndAddMarkers}"
				value="Save and continue"
			/>
			<input
				type="submit"
				class="btn btn-primary"
				name="${ManageAssignmentMappingParameters.createAndAddStudents}"
				value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>