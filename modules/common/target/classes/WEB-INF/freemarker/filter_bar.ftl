<div class="fix-area">
	<div class="fix-header pad-when-fixed">
		<@f.form commandName=filterCommandName action=submitUrl method="GET" cssClass="form-inline">
			<@f.errors cssClass="error form-errors" />
			<#--Don't send academic year if it's a path variable (therefore global in the model)-->
			<#if filterCommand.academicYear?? && !academicYear??>
				<@f.hidden path="academicYear"/>
			</#if>
			<#if filterCommand.hasBeenFiltered??>
				<@f.hidden path="hasBeenFiltered" value="true"/>
			</#if>
			<#if filterCommand.page??>
				<@f.hidden path="page" />
			</#if>
			<#if filterCommand.studentsPerPage??>
				<@f.hidden path="studentsPerPage" />
			</#if>
			<#if filterCommand.sortOrder??>
				<@f.hidden path="sortOrder" />
			</#if>

		<#-- cross-app singleton introductory text -->
			<#if showIntro("tier4-filtering", "anywhere")>
				<#assign introText>
					<p>You can now filter to view only those students who may have Tier 4 monitoring/reporting requirements by checking 'Tier 4 only' under the 'Other' tab.</p>
				</#assign>
				<a href="#"
				   id="tier4-intro"
				   class="use-introductory<#if showIntro("tier4-filtering", "anywhere")> auto</#if>"
				   data-hash="${introHash("tier4-filtering", "anywhere")}"
				   data-title="Tier 4 Filtering"
				   data-placement="bottom"
				   data-html="true"
				   data-content="${introText}"><i class="icon-question-sign fa fa-question-circle"></i></a>
			</#if>


			<div class="student-filter btn-group-group well well-small well-sm">
				<button type="button" class="clear-all-filters btn btn-link">
					<span class="icon-stack fa-stack">
						<i class="icon-filter fa fa-filter fa-stack-1x"></i>
						<i class="icon-ban-circle fa fa-ban icon-stack-base fa-stack-2x"></i>
					</span>
				</button>

				<#macro filter path placeholder currentFilter allItems validItems=allItems prefix="" customPicker="">
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
											<#local isValid = (allItems?size == validItems?size)!true />
											<#if !isValid>
												<#list validItems as validItem>
													<#if ((validItem.id)!0) == ((item.id)!0)>
														<#local isValid = true />
													</#if>
												</#list>
											</#if>
											<li class="check-list-item" data-natural-sort="${item_index}">
												<label class="checkbox <#if !isValid>disabled</#if>">
													<#nested item isValid/>
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
							<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
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

				<#assign placeholder = "All course types" />
				<#assign currentfilter><@current_filter_value "courseTypes" placeholder; courseType>${courseType.code}</@current_filter_value></#assign>
				<@filter "courseTypes" placeholder currentfilter filterCommand.allCourseTypes; courseType>
					<input type="checkbox" name="${status.expression}" value="${courseType.code}" data-short-value="${courseType.code}" ${contains_by_code(filterCommand.courseTypes, courseType)?string('checked','')}>
					${courseType.description}
				</@filter>

				<#assign placeholder = "All routes" />
				<#assign currentfilter><@current_filter_value "routes" placeholder; route>${route.code?upper_case}</@current_filter_value></#assign>
				<#assign routesCustomPicker>
					<div class="route-search input-append input-group">
						<input class="route-search-query route prevent-reload form-control" type="text" value="" placeholder="Search for a route" />
						<span class="add-on input-group-addon"><i class="icon-search fa fa-search"></i></span>
					</div>
				</#assign>
				<@filter path="routes" placeholder=placeholder currentFilter=currentfilter allItems=filterCommand.allRoutes validItems=filterCommand.visibleRoutes customPicker=routesCustomPicker; route, isValid>
					<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}" ${contains_by_code(filterCommand.routes, route)?string('checked','')} <#if !isValid>disabled</#if>>
					<@fmt.route_name route false />
				</@filter>

				<#assign placeholder = "All attendance" />
				<#assign currentfilter><@current_filter_value "modesOfAttendance" placeholder; moa>${moa.shortName?capitalize}</@current_filter_value></#assign>
				<@filter "modesOfAttendance" placeholder currentfilter filterCommand.allModesOfAttendance; moa>
					<input type="checkbox" name="${status.expression}" value="${moa.code}" data-short-value="${moa.shortName?capitalize}"
					${contains_by_code(filterCommand.modesOfAttendance, moa)?string('checked','')}>
					${moa.fullName}
				</@filter>

				<#assign placeholder = "All years" />
				<#assign currentfilter><@current_filter_value "yearsOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
				<@filter "yearsOfStudy" placeholder currentfilter filterCommand.allYearsOfStudy filterCommand.allYearsOfStudy "Year "; yearOfStudy>
					<input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
					${filterCommand.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
					${yearOfStudy}
				</@filter>

				<#assign placeholder = "All statuses" />
				<#assign currentfilter><@current_filter_value "sprStatuses" placeholder; sprStatus>${sprStatus.shortName?capitalize}</@current_filter_value></#assign>
				<@filter "sprStatuses" placeholder currentfilter filterCommand.allSprStatuses; sprStatus>
					<input type="checkbox" name="${status.expression}" value="${sprStatus.code}" data-short-value="${sprStatus.shortName?capitalize}" ${contains_by_code(filterCommand.sprStatuses, sprStatus)?string('checked','')}>
					${sprStatus.fullName}
				</@filter>

				<#assign placeholder = "All modules" />
				<#assign modulesCustomPicker>
					<div class="module-search input-append input-group">
						<input class="module-search-query module prevent-reload form-control" type="text" value="" placeholder="Search for a module" />
						<span class="add-on input-group-addon"><i class="icon-search fa fa-search"></i></span>
					</div>
				</#assign>
				<#assign currentfilter><@current_filter_value "modules" placeholder; module>${module.code?upper_case}</@current_filter_value></#assign>
				<@filter path="modules" placeholder=placeholder currentFilter=currentfilter allItems=filterCommand.allModules customPicker=modulesCustomPicker; module>
					<input type="checkbox" name="${status.expression}"
						   value="${module.code}"
						   data-short-value="${module.code?upper_case}"
							${contains_by_code(filterCommand.modules, module)?string('checked','')}>
					<@fmt.module_name module false />
				</@filter>

				<#if features.visaInStudentProfile>
					<#assign placeholder = "Other" />
					<#assign currentfilter>
						<#-- The current_filter_value macro looks to see if a list variable called otherCriteria
							- coming into the form has a value.  If it does, it lists the elements as "criterion" -->
						<@current_filter_value "otherCriteria" placeholder; criterion>
							${criterion}
						</@current_filter_value>
					</#assign>

					<@filter "otherCriteria" placeholder currentfilter filterCommand.allOtherCriteria; criterion>
						<input type="checkbox"
							name="${status.expression}"
							value="${criterion}"
							data-short-value="${criterion}"
							${filterCommand.otherCriteria?seq_contains(criterion)?string('checked','')}
						>
						${criterion}
					</@filter>
				</#if>

			</div>

			<#if filterFormAddOn?has_content>
				<#include filterFormAddOn />
			</#if>
		</@f.form>
	</div>

	<div id="filter-results">
		<#include filterResultsPath />
	</div>
</div>

<script type="text/javascript">
	jQuery(function($) {
		$('.fix-area').fixHeaderFooter();

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
									.html('<i class="icon-ban-circle fa fa-ban"></i> Clear selected items')
									.on('click', function(e) {
										$list.find('input:checked').each(function() {
											var $checkbox = $(this);
											$checkbox.prop('checked', false);
											updateFilter($checkbox);
										});

										doRequest($list.closest('form'));
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

			updateClearAllButton($el);
		};

		var updateClearAllButton = function($el) {
			var $filterList = $el.closest(".student-filter");

			if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
				$('.clear-all-filters').attr("disabled", "disabled");
			} else {
				$('.clear-all-filters').removeAttr("disabled");
			}
		};

		var doRequest = function($form, preventPageReset) {
			if (typeof history.pushState !== 'undefined')
				history.pushState(null, null, $form.attr('action') + '?' + $form.serialize());

			if ($form.data('request')) {
				$form.data('request').abort();
				$form.data('request', null);
			}

			if (!preventPageReset) {
				$form.find('input[name="page"]').val('1');
			}

			$('#filter-results').addClass('loading');
			$form.data('request', $.post($form.attr('action'), $form.serialize(), function(data) {
				$('#filter-results').html(data);

				$form.data('request', null);
				$('#filter-results').removeClass('loading');

				$('.use-wide-popover').tabulaPopover({
					trigger: 'click',
					container: '#container',
					template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
				});

				$('.use-tooltip').tooltip();

				// callback for hooking in local changes to results
				$(document).trigger("tabula.filterResultsChanged");
			}));
		};
		window.doRequest = doRequest;

		$('#${filterCommandName} .student-filter').on('change', function(e) {
			// Load the new results
			var $input = $(e.target);

			if ($input.is('.prevent-reload')) return;

			var $form = $input.closest('form');

			doRequest($form);
			updateFilter($input);
		});

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

			doRequest($('#${filterCommandName}'));
		});

		var updateFilterFromPicker = function($picker, name, value, shortValue) {
			if (value === undefined || value.length === 0)
				return;

			shortValue = shortValue || value;

			var $ul = $picker.closest('ul');

			var $li = $ul.find('input[value="' + value + '"]').closest('li');
			if ($li.length) {
				$li.find('input').prop('checked', true);
				if ($ul.find('li.check-list-item:first').find('input').val() !== value) {
					$li.insertBefore($ul.find('li.check-list-item:first'));
				}
			} else {
				$li = $('<li/>').addClass('check-list-item').append(
					$('<label/>').addClass('checkbox').append(
						$('<input/>').attr({
							'type':'checkbox',
							'name':name,
							'value':value,
							'checked':true
						}).data('short-value', shortValue)
					).append(
						$picker.val()
					)
				).insertBefore($ul.find('li.check-list-item:first'));
			}

			doRequest($picker.closest('form'));
			updateFilter($picker);
		};

		$('.route-search-query').on('change', function(){
			var $picker = $(this);
			if ($picker.data('routecode') === undefined || $picker.data('routecode').length === 0)
				return;

			updateFilterFromPicker($picker, 'routes', $picker.data('routecode'), $picker.data('routecode').toUpperCase());

			$picker.data('routecode','').val('');
		}).routePicker({});

		$('.module-search-query').on('change', function(){
			var $picker = $(this);
			if ($picker.data('modulecode') === undefined || $picker.data('modulecode').length === 0)
				return;

			updateFilterFromPicker($picker, 'modules', $picker.data('modulecode'), $picker.data('modulecode').toUpperCase());

			$picker.data('modulecode','').val('');
		}).modulePicker({});
	});
</script>