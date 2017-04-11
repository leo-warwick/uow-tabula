<@bs3form.labelled_form_group path="name" labelText="Exam name">
	<@f.input path="name" cssClass="form-control" />
</@bs3form.labelled_form_group>

<@bs3form.labelled_form_group "" "Academic year">
	<p class="form-control-static">
		<#if exam??>${exam.academicYear.toString}<#else>${academicYear.toString}</#if>
		<span class="very-subtle">(can't be changed)</span>
	</p>
</@bs3form.labelled_form_group>

<#if features.markingWorkflows>
	<@bs3form.labelled_form_group path="markingWorkflow" labelText="Marking workflow">
		<#assign disabled = !(canUpdateMarkingWorkflow!true)>
		<#if command.allMarkingWorkflows?has_content>
				<@f.select path="markingWorkflow" disabled=disabled cssClass="form-control">
					<@f.option value="" label="None"/>
					<#list command.allMarkingWorkflows as markingWorkflow>
						<@f.option value="${markingWorkflow.id}" label="${markingWorkflow.name} (${markingWorkflow.markingMethod.description})"/>
					</#list>
				</@f.select>
		<#else>
			<p class="form-control-static">
				There are no appropriate workflows available. Only single marker workflows can be used for exams.
			</p>
		</#if>
		<div class="help-block">
			<#if disabled>
				<span class="alert alert-info">You cannot change the marking workflow for this exam.</span>
			<#else>
				Marking workflows define how and by whom the exam will be marked. They are set up for the department by a Departmental Administrator <#if can.do("MarkingWorkflow.Manage", department)><a href="<@routes.exams.markingWorkflowList department />">here</a></#if>.
			</#if>
		</div>
	</@bs3form.labelled_form_group>
</#if>

<@sits_groups.exams_sits_groups command />

<@bs3form.labelled_form_group path="massAddUsers" labelText="Additional students">
	<textarea name="massAddUsers" rows="3" class="form-control">${command.originalMassAddUsers!""}</textarea>
	<div class="help-block">
		Additional students registered for the exam. Add University IDs, one per line.
	</div>
</@bs3form.labelled_form_group>