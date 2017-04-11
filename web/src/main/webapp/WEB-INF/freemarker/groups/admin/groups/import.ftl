<#import "*/group_components.ftl" as components />

<#macro groups_details timetabledEvent>
	<div class="set-info striped-section collapsible">
		<div class="clearfix">
			<div class="section-title row">
				<div class="col-md-8 icon-container">
					<span class="h6 colour-h6">${timetabledEvent.module.code?upper_case} ${timetabledEvent.eventType.displayName}s</span>
				</div>
				<div class="col-md-2">
					<@fmt.p timetabledEvent.events?size "group" />
				</div>
				<div class="col-md-2">
					<#local studentsCount = 0 />
					<#list timetabledEvent.events as event>
						<#local studentsCount = studentsCount + event.students?size />
					</#list>

					<@fmt.p studentsCount "student" />
				</div>
			</div>

			<div class="striped-section-contents">
				<#list timetabledEvent.events as event>
					<div class="item-info row">
						<div class="col-md-2">
							<h4 class="name">
								Group ${event_index + 1}
							</h4>
						</div>
						<div class="col-md-2">
							<@fmt.p event.students?size "student" />
						</div>

						<div class="col-md-8">
							<#if event.startTime??><@fmt.time event.startTime /></#if> ${(event.day.name)!""}

							<#local popoverContent><@components.timetableEventDetails event academicYear department /></#local>
							<a class="use-popover"
							   data-html="true"
							   data-content="${popoverContent?html}"><i class="fa fa-question-circle"></i></a>
						</div>
					</div>
				</#list>
			</div>
		</div>
	</div> <!-- module-info striped-section-->
</#macro>

<#escape x as x?html>
	<#function route_function dept>
		<#local result><@routes.groups.import_groups_for_year dept academicYear /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader title="Import small groups from Syllabus+" route_function=route_function preposition="for"/>

	<#assign post_url><@routes.groups.import_groups department /></#assign>
	<div class="fix-area">
		<@f.form method="post" id="import-form" action="${post_url}" commandName="command">
			<input type="hidden" name="action" value="" />

			<p>Below are all of the scheduled small groups defined for modules in this department in Syllabus+, the central timetabling system.</p>

			<p>Use the checkboxes on the left hand side to choose which ones you want to import into Tabula.
			   If there is already a set of small groups with the same name for this academic year, it will have
			   been unchecked, but you can check them if you want to import them again (note: they will be imported
			   as a <strong>separate</strong> set of small groups to the ones already imported.</p>

			<@bs3form.labelled_form_group path="academicYear" labelText="Academic year">
				<@f.select path="academicYear" id="academicYearSelect" cssClass="form-control">
					<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
				</@f.select>
			</@bs3form.labelled_form_group>

			<table class="table table-striped" id="import-groups-table">
				<tr>
					<th>
						<div class="check-all">
							<input type="checkbox" checked="checked" class="collection-check-all use-tooltip" title="Select/unselect all">
						</div>
					</th>
					<td></td>
				</tr>

				<#list timetabledEvents as timetabledEvent>
					<tr class="itemContainer">
						<td class="selectable"><@f.checkbox path="selected[${timetabledEvent_index}]" cssClass="collection-checkbox" /></td>
						<td class="selectable">
							<@groups_details timetabledEvent />
						</td>
					</tr>
				</#list>
			</table>

			<div class="fix-footer">
				<input id="submit-button" type="submit" value="Import groups" class="btn btn-primary" data-loading-text="Importing&hellip;" autocomplete="off">
				<a class="btn btn-default" href="<@routes.groups.departmenthome department academicYear />">Cancel</a>
			</div>
		</@f.form>
	</div>

	<script type="text/javascript">
		jQuery(function($){
			var $form = $('#import-form');

			// reload page when academic field dropdown changes, as it changes the contents of the list.
			$('#academicYearSelect').change(function(){
				$form.find('input[name=action]').val('change-year');
				$form.submit();
			});

			var batchTableMouseDown = false;

			$('#import-groups-table').bigList({
				onChange : function() {
					this.closest("tr").toggleClass("selected", this.is(":checked"));
				},

				onSomeChecked : function() {
					$('#submit-button').removeClass('disabled');
				},

				onNoneChecked : function() {
					$('#submit-button').addClass('disabled');
				}
			}).on('mousedown', 'td.selectable', function(){
				// cool selection mechanism...
				batchTableMouseDown = true;
				var $row = $(this).closest('tr');
				$row.toggleClass('selected');
				var checked = $row.hasClass('selected');
				$row.find('.collection-checkbox').prop('checked', checked);
				return false;
			}).on('mouseenter', 'td.selectable', function(){
				if (batchTableMouseDown) {
					var $row = $(this).closest('tr');
					$row.toggleClass('selected');
					var checked = $row.hasClass('selected');
					$row.find('.collection-checkbox').prop('checked', checked);
				}
			}).on('mousedown', 'a.name-edit-link, .striped-section, input[type="checkbox"]', function(e){
				// prevent td.selected toggling when clicking
				e.stopPropagation();
			});

			$(document).mouseup(function(){
				batchTableMouseDown = false;
				$('#import-groups-table').bigList('changed');
			});
		});
	</script>
</#escape>