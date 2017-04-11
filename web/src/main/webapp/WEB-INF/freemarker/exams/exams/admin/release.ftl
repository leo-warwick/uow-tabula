<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#assign formAction><@routes.exams.releaseForMarking exam /></#assign>

<@f.form method="post" action="${formAction}" commandName="command">

	<h1>Release ${exam.name} to markers</h1>

	<@form.errors path="" />
	<input type="hidden" name="confirmScreen" value="true" />

	<p>Please confirm you would like to release this exam for marking.</p>

	<div>
		<input class="btn btn-primary" type="submit" value="Confirm">
		<a class="btn btn-default" href="<@routes.exams.moduleHomeWithYear exam.module exam.academicYear />">Cancel</a>
	</div>

</@f.form>
</#escape>