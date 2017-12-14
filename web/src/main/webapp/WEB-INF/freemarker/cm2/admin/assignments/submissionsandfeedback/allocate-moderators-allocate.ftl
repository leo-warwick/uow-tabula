<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
<#assign action><@routes.cm2.moderationSamplingAllocation assignment /></#assign>
<@f.form cssClass="double-submit-protection" method="post" action="${action}" commandName="allocateCommand">

	<@cm2.assignmentHeader "Allocate to moderators" assignment "for" />
	<input type="hidden" name="confirmScreen" value="true" />

	<@spring.bind path="students">
		<#assign students = status.actualValue />
		<p>
			Allocating <strong><@fmt.p students?size "submission" /></strong> to moderators.
		</p>
		<#list students as student><input type="hidden" name="students" value="${student}" /></#list>
	</@spring.bind>

	<@bs3form.form_group>
		<@bs3form.checkbox path="confirm">
			<@f.checkbox path="confirm" />
				I confirm that I want to allocate these submissions to moderators.
		</@bs3form.checkbox>
	</@bs3form.form_group>

	<div class="buttons">
		<input class="btn btn-primary" type="submit" value="Confirm">
		<a class="btn btn-default" href="<@routes.cm2.moderationSampling assignment />">Cancel</a>
	</div>

</@f.form>
</#escape>