<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

	<#assign formAction><@routes.coursework.bulkApproval assignment marker /></#assign>
	<@f.form method="POST" action="${formAction}">
		<h1>Approve feedback</h1>
		<input type="hidden" name="confirmScreen" value="true" />
		<@form.errors path="" />

		<@spring.bind path="markerFeedback">
			<@form.errors path="markerFeedback" />
			<#assign markerFeedback=status.actualValue />
			<#list markerFeedback as mf>
				<input type="hidden" name="markerFeedback" value="${mf.id}" />
			</#list>
		</@spring.bind>

		<p>
			Feedback for <strong><@fmt.p markerFeedback?size "submission" /></strong> will be listed as approved.
			By approving this feedback you agree that it is ready to be published to students. If you feel that further
			modifications should be made to any feedback before it is published then request changes to the feedback instead.
		</p>

		<p>
			<@form.errors path="confirm" />
			<@form.label checkbox=true><@f.checkbox path="confirm" />
				I confirm that I approve the feedback for <@fmt.p markerFeedback?size "this" "these" "1" "0" false /> student's submissions.
			</@form.label>
		</p>

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class="btn" href="<@routes.coursework.listmarkersubmissions assignment marker />">Cancel</a>
		</div>
	</@f.form>
</#escape>