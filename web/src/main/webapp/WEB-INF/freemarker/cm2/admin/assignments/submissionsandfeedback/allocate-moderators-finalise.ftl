<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
<#assign action><@routes.cm2.moderationSamplingFinalise assignment /></#assign>
<@f.form cssClass="double-submit-protection" method="post" action="${action}" commandName="finaliseCommand">

	<@cm2.assignmentHeader "Finalise feedback" assignment "for" />
	<input type="hidden" name="confirmScreen" value="true" />

	<@spring.bind path="students">
		<#assign students = status.actualValue />
		<p>
			Finalising <strong><@fmt.p students?size "item" /></strong> of feedback.
		</p>
		<#list students as student><input type="hidden" name="students" value="${student}" /></#list>
	</@spring.bind>

	<@bs3form.form_group>
		<@bs3form.checkbox path="confirm">
			<@f.checkbox path="confirm" />
				I confirm that I want to finalise feedback for these students.
		</@bs3form.checkbox>
	</@bs3form.form_group>

	<div class="buttons">
		<input class="btn btn-primary" type="submit" value="Confirm">
		<a class="btn btn-default" href="<@routes.cm2.moderationSampling assignment />">Cancel</a>
	</div>

</@f.form>
</#escape>