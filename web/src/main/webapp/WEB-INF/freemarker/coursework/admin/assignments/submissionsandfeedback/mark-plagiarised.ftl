<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign formAction>
	<@routes.coursework.plagiarismInvestigation assignment=assignment />
</#assign>

<@f.form method="post" action="${formAction}" commandName="command">

	<#if command.markPlagiarised>
		<#assign verb = "mark">
		<#assign message = "Items marked in this way will be held back when feedback is published.">

	<#else>
		<#assign verb = "unmark">
		<#assign message = "Items will be included when feedback is published.">
	</#if>

	<h1>Suspected plagiarism for ${assignment.name}</h1>

	<@form.errors path="" />
	<@f.hidden path="markPlagiarised"  />
	<input type="hidden" name="confirmScreen" value="true" />

	<@spring.bind path="students">
		<@form.errors path="students" />
		<#assign students=status.actualValue />
		<p>
			${verb?cap_first}ing <strong><@fmt.p students?size "student" /></strong> as suspected of being plagiarised.
			${message}
		</p>
		<#list students as usercode>
			<input type="hidden" name="students" value="${usercode}" />
		</#list>
	</@spring.bind>


	<p>
		<@form.errors path="confirm" />
		<@form.label checkbox=true><@f.checkbox path="confirm" />
			<#if (students?size > 1)>
			 I confirm that I want to ${verb} these students' submissions as suspected of being plagiarised.
			<#else>
			 I confirm that I want to ${verb} this student's submission as suspected of being plagiarised.
			</#if>
		</@form.label>
	</p>

	<div class="submit-buttons">
		<input class="btn btn-warn" type="submit" value="Confirm">
	</div>
</@f.form>
</#escape>