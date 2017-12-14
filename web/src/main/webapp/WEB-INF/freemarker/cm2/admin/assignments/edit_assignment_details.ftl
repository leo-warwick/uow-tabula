<#escape x as x?html>
	<#import "*/assignment_components.ftl" as components />
	<#import "*/cm2_macros.ftl" as cm2 />

	<@cm2.assignmentHeader "Edit assignment details" assignment "for" />

<div class="fix-area">
	<#if assignment.cm2Assignment>
		<#assign actionUrl><@routes.cm2.editassignmentdetails assignment /></#assign>
	<#else>
		<#assign actionUrl><@routes.coursework.assignmentedit assignment /></#assign>
	</#if>
	<@f.form method="post" action=actionUrl cssClass="dirty-check">
		<@components.assignment_wizard 'details'  assignment.module true assignment />
		<@f.errors cssClass="error form-errors" />
		<#assign newRecord=false />
		<#include "_fields.ftl" />

		<#include "_modify_assignment_workflow.ftl" />
		<#include "_workflow_edit_warning.ftl" />

		<div class="fix-footer form-group">
			<input
							type="submit"
							class="btn btn-primary"
							name="${ManageAssignmentMappingParameters.editAndAddFeedback}"
							value="Save and continue"
			/>
			<input
							type="submit"
							class="btn btn-primary"
							name="${ManageAssignmentMappingParameters.editAndEditDetails}"
							value="Save and exit"
			/>
		</div>

		<#if can.do("Assignment.Delete", assignment)>
			<#if canDeleteAssignment>
				<p class="alert alert-info">Did you create this assignment in error?
					You may <a href="<@routes.cm2.assignmentdelete assignment=assignment />" class="btn btn-danger">delete</a> it if you definitely won't need it again.</p>
			<#else>
				<p class="alert alert-info">
					It's not possible to delete this assignment because it has submissions and/or feedback is published.
				</p>
			</#if>
		</#if>
	</@f.form>
</div>
</#escape>