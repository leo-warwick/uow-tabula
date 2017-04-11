<#escape x as x?html>
<#import "*/group_components.ftl" as components />

<#if department??>
	<#function route_function dept>
		<#local result><@routes.groups.copyDepartment dept /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader title="Copy small groups" route_function=route_function preposition="for" />
<#else>
	<div class="deptheader">
		<h1>Copy small groups</h1>
		<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
	</div>
</#if>

<#if department??>
	<#assign submitUrl><@routes.groups.copyDepartment department /></#assign>
	<#assign cancelUrl><@routes.groups.departmenthome department=department year=copySmallGroupSetsCommand.targetAcademicYear /></#assign>
<#else>
	<#assign submitUrl><@routes.groups.copyModule module /></#assign>
	<#assign cancelUrl><@routes.groups.depthome module=module academicYear=copySmallGroupSetsCommand.targetAcademicYear /></#assign>
</#if>

<div class="fix-area">
	<@f.form method="post" action=submitUrl commandName="copySmallGroupSetsCommand">
		<input type="hidden" name="action" value="submit" id="action-submit">

		<@bs3form.labelled_form_group path="sourceAcademicYear" labelText="From academic year">
			<@f.select path="sourceAcademicYear" id="sourceAcademicYear" cssClass="form-control">
				<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
			</@f.select>
		</@bs3form.labelled_form_group>

		<script type="text/javascript">
			jQuery(function($) {
				$('#sourceAcademicYear').on('change', function(e) {
					var $form = $(this).closest('form');
					$('#action-submit').val('refresh');

					$form.submit();
				});
			});
		</script>

		<@bs3form.labelled_form_group path="targetAcademicYear" labelText="To academic year">
			<@f.select path="targetAcademicYear" id="targetAcademicYear" cssClass="form-control">
				<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
			</@f.select>
		</@bs3form.labelled_form_group>

		<p>Select sets of groups that you'd wish to copy:</p>

		<table class="table table-striped small-group-sets-list" id="copy-groups-table">
			<thead>
				<tr>
					<th>
						<input type="checkbox" class="collection-check-all use-tooltip" title="Select/unselect all">
					</th>
					<th>Set name</th>
					<th>
						Copy groups?
						<a class="use-popover" data-html="true"
						   title="Copy groups"
						   data-content="Copy groups as well as the set information (these won't have any students on them)">
							<i class="icon-question-sign"></i>
						</a>
					</th>
					<th>
						Copy events?
						<a class="use-popover" data-html="true"
						   title="Copy events"
						   data-content="Copy events to the copied groups">
							<i class="icon-question-sign"></i>
						</a>
					</th>
				</tr>
			</thead>
			<tbody>
				<#list copySmallGroupSetsCommand.smallGroupSets as state>
					<tr>
						<@spring.nestedPath path="smallGroupSets[${state_index}]">
							<td><@f.checkbox path="copy" cssClass="collection-checkbox" /></td>
							<td>
								<@f.hidden path="smallGroupSet" />
								<span class="h6 colour-h6"><@fmt.groupset_name state.smallGroupSet /></span>
							</td>
							<td><@f.checkbox path="copyGroups" /></td>
							<td>
								<@f.checkbox path="copyEvents" />
								<@bs3form.errors path="copyEvents" />
							</td>
						</@spring.nestedPath>
					</tr>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons fix-footer">
			<input type="submit" class="btn btn-primary" value="Create" />
			<a class="btn btn-default dirty-check-ignore" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</div>

<script type="text/javascript">
	jQuery(function($) {
		$('input[type=checkbox][name$=copyEvents]').each(function () {
			var $eventsCb = $(this);
			var $groupsCb = $eventsCb.closest('tr').find('input[type=checkbox][name$=copyGroups]');
			var $copyCb = $eventsCb.closest('tr').find('input[type=checkbox][name$=copy]');

			$eventsCb.prop('disabled', !$groupsCb.prop('checked'));
			$groupsCb.on('change', function () {
				$eventsCb.prop('checked', $groupsCb.prop('checked'));
				$eventsCb.prop('disabled', !$groupsCb.prop('checked'));
			});

			$groupsCb.prop('disabled', !$copyCb.prop('checked'));
			$copyCb.on('change', function () {
				$groupsCb.prop('checked', $copyCb.prop('checked'));
				$groupsCb.prop('disabled', !$copyCb.prop('checked'));

				$eventsCb.prop('checked', $copyCb.prop('checked'));
				$eventsCb.prop('disabled', !$copyCb.prop('checked'));
			});
		});

		$('#copy-groups-table').bigList({
			onChange : function() {
				var $copyCb = this;
				$copyCb.closest("tr").toggleClass("selected", this.prop('checked'));

				var $eventsCb = $copyCb.closest('tr').find('input[type=checkbox][name$=copyEvents]');
				var $groupsCb = $copyCb.closest('tr').find('input[type=checkbox][name$=copyGroups]');

				$groupsCb.prop('checked', $copyCb.prop('checked'));
				$groupsCb.prop('disabled', !$copyCb.prop('checked'));

				$eventsCb.prop('checked', $copyCb.prop('checked'));
				$eventsCb.prop('disabled', !$copyCb.prop('checked'));

				var x = $('#copy-groups-table .collection-checkbox:checked').length;
				$('#selected-count').text(x+" selected");
			}
		});
	});
</script>

</#escape>