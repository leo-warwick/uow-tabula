<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.cm2.reusableWorkflowsHome dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Marking workflows" route_function=route_function preposition="for" />

<#if copiedWorkflow??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		${copiedWorkflow.name} was added to ${currentYear.toString}.
	</div>
</#if>

<#if deletedWorkflow??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		${deletedWorkflow} was deleted.
	</div>
</#if>

<#if actionErrors??>
	<div class="alert alert-danger">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		${actionErrors}
	</div>
</#if>

<#if isCurrentYear>
	<p>You can create marking workflows here and then use them with one or more assignments to define how marking is done for that assignment. Below is the list of the current workflows available to you. To use a previous workflow, click on the appropriate year and click 'add to ${currentYear.toString}'.</p>
	<@bs3form.labelled_form_group>
		<a class="btn btn-primary" href="<@routes.cm2.reusableWorkflowAdd department academicYear />">
			Create a new workflow
		</a>
	</@bs3form.labelled_form_group>
</#if>
<#if workflows?has_content>
	<#if !isCurrentYear><p>Below is the list of the ${academicYear.toString} workflows. To use any of these for the current year, click 'add to ${currentYear.toString}'.</p></#if>
	<table class="table-sortable table table-bordered table-striped">
		<thead>
		<tr>
			<th class="sortable">Name</th>
			<th class="sortable">Type</th>
			<th>Marker</th>
			<th></th>
		</tr>
		</thead>
		<tbody>
			<#list workflows as workflow>
				<#assign inUse = workflow.assignments?size &gt; 0 />
				<tr <#if copiedWorkflow?? && workflow.id == copiedWorkflow.id>class="info"</#if>>
					<td>${workflow.name}</td>
					<td>${workflow.workflowType.description}</td>
					<td>
						<#list workflow.markersByRole?keys as stage><#compress>
							<strong>${stage.roleName}: </strong>
							<#assign markers = mapGet(workflow.markersByRole, stage) />
							<#list markers as marker>
							${marker.fullName}<#if marker_has_next>, </#if>
							</#list><br />
						</#compress></#list>
					</td>
					<td>
						<#if isCurrentYear>
							<a class="btn btn-default" href="<@routes.cm2.reusableWorkflowEdit department academicYear workflow/>">Modify</a>
							<a	<#if inUse>
									disabled="disabled"
									class="btn btn-default use-tooltip"
									data-toggle=""
									title="You can't delete this marking workflow as it is in use by <@fmt.p number=workflow.assignments?size singular="assignment" />"
							<#else>
									class="btn btn-default"
									href="<@routes.cm2.reusableWorkflowDelete department academicYear workflow/>"
							</#if>
							>
								Delete
							</a>
						<#else>
							<a class="btn btn-default" href="<@routes.cm2.reusableWorkflowAddToCurrentYear department academicYear workflow/>">Add to ${currentYear.toString}</a>
						</#if>
					</td>
				</tr>
			</#list>
		</tbody>
	</table>
<#else>
	<p>There are no workflows for ${department.name} in ${academicYear.toString}.</p>
</#if>
</#escape>
