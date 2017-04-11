<#escape x as x?html>
<#assign actionUrl><@routes.cm2.editfeedbacktemplate department template /></#assign>
<@f.form enctype="multipart/form-data"
	method="post"
	class="form-horizontal"
	action="${actionUrl}"
	commandName="editFeedbackTemplateCommand">
	<@f.hidden path="id"/>
	<div class="col-md-12">
		<@bs3form.form_group path="file.upload">
			<label>Update feedback template</label>
			<input type="file" name="file.upload" />
			<div class="help-block">
				Upload a new version of the feedback template. All assignments configured to use this feedback template will
				use the new version as soon as you save. This field is optional. <#-- TODO less rubbish help notes -->
			</div>
		</@bs3form.form_group>
	</div>
	<div class="col-md-12">
		<@bs3form.form_group path="name">
			<label>Name</label><br />
			<@f.input path="name" cssClass="text" />
			<div class="help-block">
				The name that will appear when administrators are asked to choose which feedback template to use for a
				module.
			</div>
		</@bs3form.form_group>
	</div>
	<div class="col-md-12">
		<@bs3form.form_group path="description">
			<label>Description</label><br />
			<@f.textarea path="description" class="big-textarea" />
			<div class="help-block">
				This field is to help administrators select appropriate feedback templates for their assignments.
				It will not be visible to students. You can leave this blank if you wish.
			</div>
		</@bs3form.form_group>
	</div>
</@f.form>
</#escape>