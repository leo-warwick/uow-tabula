<@bs3form.labelled_form_group path="absenceType" labelText="Absence type">
	<@f.select path="absenceType" cssClass="form-control">
	<option value="" style="display: none;">Please select one&hellip;</option>
		<#list allAbsenceTypes as type>
			<@f.option value="${type.dbValue}" label="${type.description}" />
		</#list>
	</@f.select>
</@bs3form.labelled_form_group>

<@bs3form.labelled_form_group path="note" labelText="Note">
	<@f.textarea path="note" cssClass="form-control" rows="5" cssStyle="height: 150px;" />
</@bs3form.labelled_form_group>

<#if command.attachedFile?has_content>
	<@bs3form.labelled_form_group path="attachedFile" labelText="Attached file">
		<@fmt.download_link
			filePath="/attendance/note/${academicYear.startYear?c}/${command.student.universityId}/${command.point.id}/attachment/${command.attachedFile.name}"
			mimeType=command.attachedFile.mimeType
			title="Download file ${command.attachedFile.name}"
			text="Download ${command.attachedFile.name}"
		/>
		&nbsp;
		<@f.hidden path="attachedFile" value="${command.attachedFile.id}" />
		<i class="fa fa-times-circle remove-attachment"></i>

		<small class="very-subtle help-block">
			This is the file attached to this attendance note.
			Click the remove link next to a document to delete it.
		</small>

	</@bs3form.labelled_form_group>

<script>
	jQuery(function($){
		$(".remove-attachment").on("click", function(){
			$(this).closest('form').find('.attendance-file').show();
			$(this).closest(".form-group").remove();
			return false;
		});
	});
</script>
</#if>

<div class="attendance-file" <#if command.attachedFile?has_content>style="display:none;"</#if>>
	<@bs3form.filewidget basename="file" types=[] multiple=false />
</div>