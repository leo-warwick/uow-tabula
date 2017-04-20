<#import "*/assignment_components.ftl" as components />
<#escape x as x?html>

<div class="deptheader">
	<#if mode == 'new'>
		<#assign assignmentHeaderText='Create a new assignment' />
		<#assign assignmentEditMode=false />
	<#else>
		<#assign assignmentHeaderText='Edit assignment' />
		<#assign assignmentEditMode=true />
	</#if>

    <h1>${assignmentHeaderText}</h1>
    <h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area">
	<#assign actionUrl><@routes.cm2.assignmentoptions assignment mode /></#assign>
	<@f.form method="post" action=actionUrl cssClass="dirty-check">
		<@components.assignment_wizard 'options' assignment.module assignmentEditMode assignment />
		<#include "_options_fields.ftl" />
        <div class="fix-footer">
            <input
                    type="submit"
                    class="btn btn-primary"
                    name="${ManageAssignmentMappingParameters.reviewAssignment}"
                    value="Save and continue"
            />
			<#if mode == 'new'>
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.createAndAddOptions}"
                        value="Save and exit"
                />
			<#else>
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.editAndAddOptions}"
                        value="Save and exit"
                />
			</#if>
        </div>
	</@f.form>
</div>
</#escape>
