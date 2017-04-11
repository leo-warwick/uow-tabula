<#escape x as x?html>
	<#assign maxFileAttachments=command.maxFileAttachments />
	<@bs3form.labelled_form_group path="minimumFileAttachmentLimit" labelText="Minimum attachments per submission">
		<@f.select path="minimumFileAttachmentLimit" cssClass="form-control">
		<@f.options items=1..maxFileAttachments />
	</@f.select>
	</@bs3form.labelled_form_group>
	<@bs3form.labelled_form_group path="fileAttachmentLimit" labelText="Maximum attachments per submission">
		<@f.select path="fileAttachmentLimit" cssClass="form-control">
		<@f.options items=1..maxFileAttachments />
	</@f.select>
	</@bs3form.labelled_form_group>
	<@bs3form.labelled_form_group path="fileAttachmentTypes" labelText="Attachment file types">
		<@f.errors path="fileAttachmentTypes" cssClass="error" />
		<@f.input path="fileAttachmentTypes"  type="hidden" />

		<script type="text/javascript">
			jQuery(function($){
				var textListController = new TextListController('#fileExtensionList', '#fileAttachmentTypes');
				textListController.transformInput = function(text){
					var result = text.replace(new RegExp('\\.', 'g') , '');
					return result.toLowerCase();
				};
				textListController.preventDuplicates = true;
				textListController.init();
			});
		</script>
		<div id="fileExtensionList" class="textBoxListContainer">
			<ul>
				<li class="inputContainer"><input class="text form-control" type="text"></li>
			</ul>
		</div>
		<div class="help-block">
			Enter the file types you would like to allow separated by spaces (e.g. "pdf doc docx"). Only attachments with the extensions
			specified will be permitted. Leave this field blank to accept attachments with any extension.
		</div>
	</@bs3form.labelled_form_group>
	<@bs3form.labelled_form_group path="individualFileSizeLimit" labelText="Maximum file size">
	<div class="input-group">
		<@f.input path="individualFileSizeLimit" cssClass="form-control" />
		<span class="input-group-addon">MB</span>
	</div>
	<div class="help-block">
		Enter the maximum file size (in Megabytes) for a single uploaded file.  If you wish to submit the file(s) to Turnitin each file can be no larger than ${turnitinFileSizeLimit}MB
	</div>
	</@bs3form.labelled_form_group>
	<@bs3form.labelled_form_group path="wordCountMin" labelText="Minimum word count">
		<@f.input path="wordCountMin" cssClass="form-control" id="wordCountMin" />
	</@bs3form.labelled_form_group>
	<@bs3form.labelled_form_group path="wordCountMax" labelText="Maximum word count">
		<@f.input path="wordCountMax" cssClass="form-control" id="wordCountMax" />
	<div class="help-block">
		If you specify a minimum and/or maximum word count, students will be required to declare the word count for their submissions. They will not be able to submit unless
		their declaration is within your specified range. If you don't specify a minimum and/or maximum, students will not be asked to declare a word count. Note that Tabula
		does not actually check the number of words in submissions. Students can submit work with any number of words.
	</div>
	</@bs3form.labelled_form_group>
	<@bs3form.labelled_form_group path="wordCountConventions" labelText="Word count conventions">
		<@f.textarea path="wordCountConventions" id="wordCountConventions" rows="6" cssClass="form-control col-md-6" />
	<div class="help-block">
		Tell students if there are specific things which should be included or excluded from the word count.
	</div>
	</@bs3form.labelled_form_group>
	<@bs3form.labelled_form_group path="comment" labelText="Text to show on submission form">
		<@f.textarea path="comment" id="assignmentComment" rows="6" cssClass="form-control col-md-6" />
	<div class="help-block">
		You can make a new paragraph by leaving a blank line (i.e. press Enter twice).
	</div>
	</@bs3form.labelled_form_group>
</#escape>
