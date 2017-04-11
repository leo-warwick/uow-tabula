<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.groups.import_spreadsheet dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Import small groups from spreadsheet" route_function "for" />

<p>You can create and edit sets of small groups, their groups and events by uploading a spreadsheet.
	Note that the spreadsheet must be in the correct format, for this reason we suggest you
	<a href="<@routes.groups.import_spreadsheet_template department academicYear />">download a template spreadsheet</a> that contains all the current groups for this year, and use that
	to create your spreadsheet.</p>

<div class="alert alert-info">
	<p>You can add extra sheets to the template and upload it, but you must upload a
		spreadsheet with three sheets: "Sets", "Groups" and "Events" in the same format as the template, and
		they must include the columns in the template (though you can add extra columns if you want, they'll be ignored).</p>
</div>

<@f.form method="post" enctype="multipart/form-data" action="" commandName="command" cssClass="double-submit-protection">
	<@bs3form.filewidget basename="file" types=[] multiple=false labelText="Choose your spreadsheet" />

	<div class="fix-footer">
		<button type="submit" class="btn btn-primary">Upload spreadsheet</button>
		<a class="btn btn-default" href="<@routes.groups.departmenthome department academicYear />">Cancel</a>
	</div>
</@f.form>

</#escape>