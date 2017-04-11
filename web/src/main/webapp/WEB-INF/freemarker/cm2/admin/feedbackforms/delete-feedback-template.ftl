<#escape x as x?html>
<#assign actionUrl><@routes.cm2.deletefeedbacktemplate department template /></#assign>
<@f.form
	method="post"
	class="form-inline"
	action="${actionUrl}"
	commandName="deleteFeedbackTemplateCommand">
	<@f.hidden path="id"/>
	<p>Are you sure that you want to delete this feedback template?</p>
</@f.form>
</#escape>