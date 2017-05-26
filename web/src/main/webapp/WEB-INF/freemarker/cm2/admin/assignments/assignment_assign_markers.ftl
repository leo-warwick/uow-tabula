<#escape x as x?html>
<#import "*/assignment_components.ftl" as components />
<#import "*/cm2_macros.ftl" as cm2 />
<#include "assign_marker_macros.ftl" />

<@cm2.moduleHeader "Create a new assignment" module "for" />

<div class="fix-area">
	<#assign actionUrl><@routes.cm2.assignmentmarkers assignment mode /></#assign>
	<@f.form method="post" action=actionUrl  cssClass="dirty-check">
		<@components.assignment_wizard 'markers' assignment.module false assignment />

		<p class="btn-toolbar">
			<a class="return-items btn btn-default" href="<@routes.cm2.assignmentmarkerstemplate assignment mode />" >
				Upload spreadsheet
			</a>
			<a class="return-items btn btn-default" href="<@routes.cm2.assignmentmarkerssmallgroups assignment mode />" >
				Import small groups
			</a>
		</p>

		<@f.errors cssClass="error form-errors" />
		<#list state.keys as roleOrStage>
			<@allocateStudents assignment roleOrStage mapGet(stages, roleOrStage)![roleOrStage] mapGet(state.markers, roleOrStage) mapGet(state.unallocatedStudents, roleOrStage) mapGet(state.allocations, roleOrStage) />
		</#list>
		<div class="fix-footer">
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddSubmissions}"
					value="Save and continue"
			/>
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddMarkers}"
					value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>