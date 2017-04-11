<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<div id="container">
	<@f.form
		method="post"
		class="form-horizontal"
		action="${url('/coursework/admin/department/${department.code}/settings/feedback-templates/delete/${template.id}')}"
		commandName="deleteFeedbackTemplateCommand">
		<@f.hidden path="id"/>
		<p>Are you sure that you want to delete this feedback template?</p>
	</@f.form>
</div>
</#escape>