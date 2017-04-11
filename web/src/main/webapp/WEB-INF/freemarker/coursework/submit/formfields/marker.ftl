<#assign markers=field.markers />
<@form.labelled_row "fields[${field.id}]" "Select your marker">
	<@f.select id="fields[${field.id}].value" path="fields[${field.id}].value">
		<@f.option value="" label="None"/>
		<#list markers as marker>
			<@f.option value="${marker.userId}" label="${marker.fullName}"/>
		</#list>
	</@f.select>
   <@f.errors path="fields[${field.id}].value" cssClass="error"/>
	<div class="help-block">
		Select the tutor that will mark your submission. If your are unsure which tutor should mark your work then contact
		the module convenor.
	</div>
</@form.labelled_row>