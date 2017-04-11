<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<div id="container">
<@f.form enctype="multipart/form-data"
	method="post"
	class="form-horizontal"
	action="${url('/coursework/admin/department/${department.code}/settings/feedback-templates/edit/${template.id}')}"
	commandName="editFeedbackTemplateCommand">
		<@f.hidden path="id"/>
		<@form.labelled_row "file.upload" "Update feedback template">
			<input type="file" name="file.upload" />
			<div  class="help-block">
				Upload a new version of the feedback template. All assignments configured to use this feedback template will
				use the new version as soon as you save. This field is optional. <#-- TODO less rubbish help notes -->
			</div>
		</@form.labelled_row>
		<@form.labelled_row "name" "Name">
			<@f.input path="name" cssClass="text" />
			<div class="help-block">
				The name that will appear when administrators are asked to choose which feedback template to use for a
				module.
			</div>
		</@form.labelled_row>
		<@form.labelled_row "description" "Description">
			<@f.textarea path="description" class="big-textarea" />
			<div class="help-block">
				This field is to help administrators select appropriate feedback templates for their assignments.
				It will not be visible to students. You can leave this blank if you wish.
			</div>
		</@form.labelled_row>
</@f.form>
</div>
</#escape>