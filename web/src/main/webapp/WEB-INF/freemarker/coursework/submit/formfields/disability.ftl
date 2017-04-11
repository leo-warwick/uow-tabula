<#if features.disabilityOnSubmission>
	<@form.labelled_row "useDisability" "Show disability">
		<@f.select id="useDisability" path="useDisability">
			<option disabled value="" <#if !submitAssignmentCommand.useDisability??>selected</#if>></option>
			<option value="true" <#if submitAssignmentCommand.useDisability?? && submitAssignmentCommand.useDisability>selected</#if>>Yes</option>
			<option value="false" <#if submitAssignmentCommand.useDisability?? && !submitAssignmentCommand.useDisability>selected</#if>>No</option>
		</@f.select>
		<div class="help-block">
			Make the marker of this submission aware of your disability and take it into consideration.
		</div>
	</@form.labelled_row>
</#if>