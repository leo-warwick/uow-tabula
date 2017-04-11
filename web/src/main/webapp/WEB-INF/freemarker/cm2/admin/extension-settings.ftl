<#escape x as x?html>
	<#function route_function department>
		<#local result><@routes.cm2.extensionSettings department /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader "Extension Settings" route_function "for" />

<div class="fix-area">
	<#assign actionUrl><@routes.cm2.extensionSettings department /></#assign>
	<@f.form method="post" class="form-inline" action=actionUrl commandName="extensionSettingsCommand" cssClass="dirty-check">
		<#if features.extensions>
			<@bs3form.labelled_form_group>
				<@bs3form.checkbox path="allowExtensionRequests">
					<@f.checkbox path="allowExtensionRequests" id="allowExtensionRequests" />
				Allow students to request extensions
				</@bs3form.checkbox>
				<@bs3form.errors path="allowExtensionRequests" />
			</@bs3form.labelled_form_group>
			<div id="request-extension-fields" style="display:none;">
				<@bs3form.labelled_form_group>
					<@f.textarea path="extensionGuidelineSummary" cssClass="form-control text big-textarea" maxlength=4000 />
					Summary of extension guidelines:
					<div class="help-block">This should be a short outline of the department's extension guidelines. Use the field below to provide
						a link to the full guidelines. You can leave this blank if you wish. You can make a new paragraph by
						leaving a blank line (i.e. press Enter twice).</div>
				</@bs3form.labelled_form_group>
				<@bs3form.labelled_form_group path="extensionGuidelineLink" labelText="Link to full extension guidelines:">
					<@f.input path="extensionGuidelineLink" cssClass="form-control text big-input" />
					<div class="help-block">
					Copy the URL of a page or file containing your full extension guidelines and paste it here.
				</div>
				</@bs3form.labelled_form_group>

				<@bs3form.labelled_form_group>
					<@bs3form.flexipicker name="extensionManagers" placeholder="Extension managers" list=true multiple=true auto_multiple=false />
					<script>
						jQuery('#extension-manager-list').on('click', function(e){
							e.preventDefault();
							var name = jQuery(this).data('expression');
							var newButton = jQuery('<div><input type="text" class="text" name="'+name+'" /></div>');
							jQuery('#extension-manager-list button').before(newButton);
							return false;
						});
					</script>
				</@bs3form.labelled_form_group>
			</div>
		</#if>
		<div class="submit-buttons form-actions">
			<input type="submit" value="Save" class="btn btn-primary">
			<#assign cancelDestination><@routes.cm2.departmenthome department /></#assign>
			<a class="btn btn-default" href="${cancelDestination}">Cancel</a>
		</div>

	</@f.form>
</#escape>