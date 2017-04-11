<#import 'form_fields.ftl' as form_fields />
<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<h2>Select courses for this grid</h2>

<p class="progress-arrows">
	<span class="arrow-right active">Select courses</span>
	<span class="arrow-right arrow-left">Set grid options</span>
	<span class="arrow-right arrow-left">Preview and download</span>
</p>

<div class="alert alert-info">
	<h3>Before you start</h3>
	<p>Exam grids in Tabula are generated using data stored in SITS.
		Before you create a new grid, ensure you have entered all the necessary data in SITS and verified its accuracy.</p>
</div>

<form action="<@routes.exams.generateGrid department academicYear />" class="form-inline select-course" method="post">

	<@form_fields.grid_options_fields />

	<div class="well well-sm filters">
		<button type="button" class="clear-all-filters btn btn-link">
			<span class="fa-stack">
				<i class="fa fa-filter fa-stack-1x"></i>
				<i class="fa fa-ban fa-stack-2x"></i>
			</span>
		</button>

		<#macro filter path placeholder currentFilter allItems prefix="" customPicker="">
			<@spring.bind path=path>
				<div class="btn-group<#if currentFilter == placeholder> empty-filter</#if>">
					<a class="btn btn-default btn-mini btn-xs dropdown-toggle" data-toggle="dropdown">
						<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
						<span class="caret"></span>
					</a>
					<div class="dropdown-menu filter-list">
						<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
						<ul>
							<#if customPicker?has_content>
								<li>
									${customPicker}
								</li>
							</#if>
							<#if allItems?has_content>
								<#list allItems as item>
									<li class="check-list-item" data-natural-sort="${item_index}">
										<#nested item />
										<label class="checkbox">

										</label>
									</li>
								</#list>
							<#else>
								<li><small class="muted" style="padding-left: 5px;">N/A for this department</small></li>
							</#if>
						</ul>
					</div>
				</div>
			</@spring.bind>
		</#macro>

		<#macro current_filter_value path placeholder><#compress>
			<@spring.bind path=path>
				<#if status.actualValue?has_content>
					<#if status.actualValue?is_collection>
						<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
					<#else>
						<#nested status.actualValue />
					</#if>
				<#else>
					${placeholder}
				</#if>
			</@spring.bind>
		</#compress></#macro>

		<#function contains_by_code collection item>
			<#list collection as c>
				<#if c.code == item.code>
					<#return true />
				</#if>
			</#list>
			<#return false />
		</#function>

		<#assign placeholder = "Course" />
		<#assign currentfilter><@current_filter_value "selectCourseCommand.course" placeholder; course>${course.code} ${course.name}</@current_filter_value></#assign>
		<@filter "selectCourseCommand.course" placeholder currentfilter selectCourseCommand.allCourses; course>
			<label class="radio">
				<input type="radio" name="${status.expression}" value="${course.code}" data-short-value="${course.code}"
					${(((selectCourseCommand.course.code)!'') == course.code)?string('checked','')}
				>
				${course.code} ${course.name}
			</label>
		</@filter>

		<#assign placeholder = "All routes" />

		<#assign currentfilter><#compress><@current_filter_value "selectCourseCommand.routes" placeholder; route>
			<#if route?is_sequence>
				<#list route as r>${r.code?upper_case}<#if r_has_next>, </#if></#list>
			<#else>
				${route.code?upper_case}
			</#if>
		</@current_filter_value></#compress></#assign>
		<@filter "selectCourseCommand.routes" placeholder currentfilter selectCourseCommand.allRoutes; route>
			<label class="checkbox">
				<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}"
					${contains_by_code(selectCourseCommand.routes, route)?string('checked','')}
				>
				${route.code?upper_case} ${route.name}
			</label>
		</@filter>

		<#assign placeholder = "Year of study" />
		<#assign currentfilter><@current_filter_value "selectCourseCommand.yearOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
		<@filter "selectCourseCommand.yearOfStudy" placeholder currentfilter selectCourseCommand.allYearsOfStudy "Year "; yearOfStudy>
			<label class="radio">
				<input type="radio" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
					${(((selectCourseCommand.yearOfStudy)!0) == yearOfStudy)?string('checked','')}
				>
				${yearOfStudy}
			</label>
		</@filter>
	</div>

	<p>
		<@bs3form.checkbox path="selectCourseCommand.includeTempWithdrawn">
			<@f.checkbox path="selectCourseCommand.includeTempWithdrawn" /> Show temporarily withdrawn students
		</@bs3form.checkbox>
	</p>

	<@bs3form.errors path="selectCourseCommand" />

	<div class="buttons">

		<button class="btn btn-default" name="${GenerateExamGridMappingParameters.selectCourse}" type="submit" disabled>Configure grid options</button>

		<button class="btn btn-default" name="${GenerateExamGridMappingParameters.usePreviousSettings}" type="submit" disabled>Generate using previous settings</button>

		<#assign popover>
			<ul>
				<#list gridOptionsCommand.predefinedColumnDescriptions as column>
					<li>${column}</li>
				</#list>
				<#list gridOptionsCommand.customColumnTitles as column>
					<li>Additional: ${column}</li>
				</#list>
				<#if gridOptionsCommand.nameToShow == 'full'>
					<li>Official name</li>
				<#else>
					<li>First and last name</li>
				</#if>
				<#if gridOptionsCommand.yearsToShow == 'current'>
					<li>Current year</li>
				<#else>
					<li>All years</li>
				</#if>
				<#if gridOptionsCommand.marksToShow == 'overall'>
					<li>Only show overall mark</li>
				<#else>
					<li>Show component marks</li>
				</#if>
				<#if gridOptionsCommand.moduleNameToShow == 'codeOnly'>
					<li>Show module code only</li>
				<#else>
					<li>Show module names</li>
				</#if>
			</ul>
		</#assign>

		<@fmt.help_popover id="gridOptions" title="Previous grid options" content=popover html=true />

	</div>
</form>

<script>
	jQuery(function($){
		var prependClearLink = function($list) {
			if (!$list.find('input:checked').length) {
				$list.find('.clear-this-filter').remove();
			} else {
				if (!$list.find('.clear-this-filter').length) {
					$list.find('> ul').prepend(
						$('<li />').addClass('clear-this-filter')
							.append(
								$('<button />').attr('type', 'button')
									.addClass('btn btn-link')
									.html('<i class="fa fa-ban"></i> Clear selected items')
									.on('click', function(e) {
										$list.find('input:checked').each(function() {
											var $checkbox = $(this);
											$checkbox.prop('checked', false);
											updateFilter($checkbox);
										});
									})
							)
							.append($('<hr />'))
					);
				}
			}
		};

		var updateFilter = function($el) {
			// Update the filter content
			var $list = $el.closest('ul');
			var shortValues = $list.find(':checked').map(function() { return $(this).data('short-value'); }).get();
			var $fsv = $el.closest('.btn-group').find('.filter-short-values');
			if (shortValues.length) {
				$el.closest('.btn-group').removeClass('empty-filter');
				$fsv.html($fsv.data("prefix") + shortValues.join(', '));
			} else {
				$el.closest('.btn-group').addClass('empty-filter');
				$fsv.html($fsv.data('placeholder'));
			}

			updateButtons($el);
		};

		var updateButtons = function($el) {
			var $filterList = $el.closest(".filters");

			if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
				$('.clear-all-filters').attr("disabled", "disabled");

			} else {
				$('.clear-all-filters').removeAttr("disabled");
			}

			var course = $('[name=course]:checked');
			var yearOfStudy = $('[name=yearOfStudy]:checked');

			if (course.length === 0 || yearOfStudy.length === 0) {
				$('.buttons button.btn-default').prop('disabled', true);
			} else {
				$('.buttons button.btn-default').prop('disabled', false);
			}
		};

		$('form.select-course .filters').on('change', function(e) {
			updateFilter($(e.target));
		});
		$('form.select-course .filters .filter-list').find('input:first').trigger('change');

		// Re-order elements inside the dropdown when opened
		$('.filter-list').closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function(e) {
			var $this = $(this);
			if (!$this.closest('.btn-group').hasClass('open')) {
				// Re-order before it's opened!
				var $list = $this.closest('.btn-group').find('.filter-list');
				var items = $list.find('li.check-list-item').get();

				items.sort(function(a, b) {
					var aChecked = $(a).find('input').is(':checked');
					var bChecked = $(b).find('input').is(':checked');

					if (aChecked && !bChecked) return -1;
					else if (!aChecked && bChecked) return 1;
					else return $(a).data('natural-sort') - $(b).data('natural-sort');
				});

				$.each(items, function(item, el) {
					$list.find('> ul').append(el);
				});

				prependClearLink($list);
			}
		});

		$('.clear-all-filters').on('click', function() {
			$('.filter-list').each(function() {
				var $list = $(this);

				$list.find('input:checked').each(function() {
					var $checkbox = $(this);
					$checkbox.prop('checked', false);
					updateFilter($checkbox);
				});

				prependClearLink($list);
			});
		});
	});
</script>
</#escape>