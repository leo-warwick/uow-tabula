<#escape x as x?html>
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<h1>Marking workflows<#if isExams> available for exams</#if></h1>

<#if isExams>
	<#assign assessmentType='exam'/>
<#else>
	<#assign assessmentType='assignment'/>
</#if>

<p>You can create marking workflows here and then
	use them with one or more ${assessmentType}s to define how marking is done for that ${assessmentType}.</p>

<#if !markingWorkflowInfo?has_content>
	<p>No marking workflows have been created yet. Click <strong>Create</strong> below to make one.</p>
</#if>

<p><a class="btn btn-default" href="<@routes.exams.markingWorkflowAdd department />">Create</a></p>

<#if markingWorkflowInfo?has_content>
	<table class="marking-workflows table table-striped">
		<thead>
			<tr>
				<th>Name</th>
				<th>Type</th>
				<th></th>
			</tr>
		</thead>
		<tbody>
			<#list markingWorkflowInfo as info>
				<#assign markingWorkflow = info.markingWorkflow />
				<#assign usedInAssignments = (info.assignmentCount > 0) />
				<#assign usedInExams = (info.examCount > 0) />

				<#if usedInAssignments>
					<#assign formattedAssignmentCount><@fmt.p info.assignmentCount "assignment" "assignments" /></#assign>
				</#if>
				<#if usedInExams>
					<#assign formattedExamCount><@fmt.p info.examCount "exam" /></#assign>
				</#if>

				<tr>
					<td>${markingWorkflow.name}</td>
					<td>${markingWorkflow.markingMethod.description}</td>
					<td>
						<a class="btn btn-xs btn-default" href="<@routes.exams.markingWorkflowEdit department markingWorkflow />">Modify</a>
						<#if usedInAssignments || usedInExams>
							<#assign title>
								You can't delete this marking workflow as it is in use by
								<#if usedInAssignments>${formattedAssignmentCount}<#if usedInExams> and </#if></#if>
								<#if usedInExams>${formattedExamCount}</#if>
							</#assign>
							<span class="btn btn-xs btn-danger use-tooltip disabled" title="${title}">
								<a class="btn-danger disabled">Delete</a>
							</span>
						<#else>
							<a class="btn btn-xs btn-danger use-tooltip ajax-modal" href="<@routes.exams.markingWorkflowDelete department markingWorkflow />" data-target="#marking-workflow-modal">Delete</a>
						</#if>

					</td>
				</tr>
			</#list>
		</tbody>
	</table>
</#if>

<div id="marking-workflow-modal" class="modal fade">
	<@modal.wrapper>
		<@modal.header>
			<h3 class="modal-title">Delete marking workflow</h3>
		</@modal.header>
		<@modal.body></@modal.body>
	</@modal.wrapper>
</div>

</#escape>