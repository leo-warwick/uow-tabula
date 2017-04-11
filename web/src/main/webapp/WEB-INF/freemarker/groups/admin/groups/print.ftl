<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<#function route_function dept>
	<#local result><@routes.groups.printRegisters dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Print registers" route_function "for" />

<div class="fix-area">
	<#assign submitUrl><@routes.groups.printRegisters department academicYear /></#assign>
	<@f.form method="post" action="${submitUrl}.pdf" commandName="command">

		<@bs3form.errors path="" />

		<p>Select the date range for events (both are inclusive):</p>

		<@bs3form.labelled_form_group path="startDate" labelText="Start date">
			<@f.input path="startDate" cssClass="form-control date-time-picker" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="endDate" labelText="End date">
			<@f.input path="endDate" cssClass="form-control date-time-picker" />
		</@bs3form.labelled_form_group>

		<p>Select sets of groups that you'd wish to print:</p>

		<@bs3form.errors path="smallGroupSets" />

		<table class="table table-striped small-group-sets-list">
			<thead>
			<tr>
				<th>
					<input type="checkbox" class="collection-check-all use-tooltip" title="Select/unselect all">
				</th>
				<th>Set name</th>
			</tr>
			</thead>
			<tbody>
				<#list command.smallGroupsInDepartment as set>
					<tr>
						<td><input class="collection-checkbox" type="checkbox" name="smallGroupSets" value="${set.id}" <#if command.smallGroupSetIds?seq_contains(set.id)>checked</#if> /></td>
						<td><span class="h6 colour-h6"><@fmt.groupset_name set /></span></td>
					</tr>
				</#list>
			</tbody>
		</table>

		<div id="print-modal" class="modal fade">
			<@modal.wrapper>
				<@modal.header>
					<h3 class="modal-title">Print register</h3>
				</@modal.header>
				<@modal.body>
					<@bs3form.checkbox>
						<input type="hidden" name="_showPhotos" value="" />
						<input type="checkbox" name="showPhotos" value="true" <#if (userSetting('registerPdfShowPhotos')!'t') == 't'>checked</#if> />
						Show student photos
					</@bs3form.checkbox>

					<#assign displayName = userSetting('registerPdfDisplayName')!'name' />
					<@bs3form.labelled_form_group "" "Name display">
						<@bs3form.radio>
							<input type="radio" name="displayName" value="name" <#if displayName == 'name'>checked</#if> />
							Show student name only
						</@bs3form.radio>
						<@bs3form.radio>
							<input type="radio" name="displayName" value="id" <#if displayName == 'id'>checked</#if> />
							Show University ID only
						</@bs3form.radio>
						<@bs3form.radio>
							<input type="radio" name="displayName" value="both" <#if displayName == 'both'>checked</#if> />
							Show both name and University ID
						</@bs3form.radio>
					</@bs3form.labelled_form_group>

					<#assign displayCheck = userSetting('registerPdfDisplayCheck')!'checkbox' />
					<@bs3form.labelled_form_group "" "Fill area">
						<@bs3form.radio>
							<input type="radio" name="displayCheck" value="checkbox" <#if displayCheck == 'checkbox'>checked</#if> />
							Include checkbox
						</@bs3form.radio>
						<@bs3form.radio>
							<input type="radio" name="displayCheck" value="line" <#if displayCheck == 'line'>checked</#if> />
							Include signature line
						</@bs3form.radio>
					</@bs3form.labelled_form_group>

					<#assign sortOrder = userSetting('registerPdfSortOrder')!'module' />
					<@bs3form.labelled_form_group "" "Sort order">
						<@bs3form.radio>
							<input type="radio" name="sortOrder" value="module" <#if sortOrder == 'module'>checked</#if> />
							Sort by module
						</@bs3form.radio>
						<@bs3form.radio>
							<input type="radio" name="sortOrder" value="tutor" <#if sortOrder == 'tutor'>checked</#if> />
							Sort by event tutor
						</@bs3form.radio>
					</@bs3form.labelled_form_group>
				</@modal.body>
				<@modal.footer>
					<button type="submit" class="btn btn-primary long-running">Save as PDF for printing</button>
					<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Close</button>
				</@modal.footer>
			</@modal.wrapper>
		</div>

		<div class="submit-buttons fix-footer">
			<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#print-modal">Print</button>
			<a class="btn btn-default" href="<@routes.groups.departmenthome department academicYear />">Done</a>
		</div>
	</@f.form>
</div>

<script type="text/javascript">
	(function ($) {
		$('.small-group-sets-list').bigList({});
	} (jQuery));
</script>

</#escape>