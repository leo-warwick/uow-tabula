<#import 'form_fields.ftl' as form_fields />
<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<form action="<@routes.exams.generateGrid department academicYear />" class="dirty-check" method="post">

	<@form_fields.select_course_fields />
	<@form_fields.grid_options_fields />

	<h2>Set grid options</h2>

	<p class="progress-arrows">
		<span class="arrow-right"><button type="submit" class="btn btn-link">Select courses</button></span>
		<span class="arrow-right arrow-left active">Set grid options</span>
		<span class="arrow-right arrow-left">Preview and download</span>
	</p>

	<div class="alert alert-info">
		<h3>Core required modules</h3>
		<p>Unfortunately Tabula cannot identify core required modules within SITS. Please select the modules for each route from the list below.</p>
		<p><strong>Note:</strong> The chosen modules will apply to all students on each route for year of study ${selectCourseCommand.yearOfStudy} and ${academicYear.toString}</p>
	</div>

	<h3>Identify Core Required Modules</h3>

	<#list coreRequiredModulesCommand.allModules?keys?sort_by('code') as route>
		<div class="striped-section collapsible <#if coreRequiredModulesCommand.allModules?keys?size == 1>expanded</#if>">
			<h4 class="section-title" title="Expand">
				${route.code?upper_case} ${route.name}
			</h4>

			<div class="striped-section-contents">
				<div class="item-info">
					<table class="table table-condensed table-striped modules">
						<thead>
							<tr>
								<th class="for-check-all" style="width: 20px;"></th>
								<th>Available Modules</th>
							</tr>
						</thead>
						<tbody>
							<#list mapGet(coreRequiredModulesCommand.allModules, route) as module>
								<#assign isChecked = false />
								<#list (mapGet(coreRequiredModulesCommand.modules, route))![] as checkedModule>
									<#if checkedModule.code == module.code>
										<#assign isChecked = true />
									</#if>
								</#list>
								<tr>
									<td><input type="checkbox" name="modules[${route.code}]" value="${module.code}" id="module-${module.code}" <#if isChecked>checked</#if>></td>
									<td><label for="module-${module.code}"> ${module.code?upper_case} ${module.name}</label></td>
								</tr>
							</#list>
						</tbody>
					</table>

					<@bs3form.errors path="coreRequiredModulesCommand.modules[${route.code}]" />
				</div>
			</div>
		</div>
	</#list>

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.coreRequiredModules}">Next</button>
</form>

<script>
	jQuery(function($){
		var updateCheckboxes = function($table) {
			var checked = $table.find('td input:checked').length;
			if (checked == $table.find('td input').length) {
				$table.find('.check-all').prop('checked', true);
			}
			if (checked == 0) {
				$table.find('.check-all').prop('checked', false);
			}
		};

		$('.for-check-all').append($('<input />', { type: 'checkbox', 'class': 'check-all use-tooltip', title: 'Select all/none' }));
		$('table.modules').on('click', '.check-all', function() {
			var checkStatus = this.checked;
			var $table = $(this).closest('table.modules');
			$table.find('td input:checkbox').prop('checked', checkStatus);

			updateCheckboxes($table);
		});
	});
</script>

</#escape>