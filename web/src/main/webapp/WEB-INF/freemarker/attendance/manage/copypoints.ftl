<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />

<#if command.schemes?size == 1>

	<h1>Copy points to scheme: ${command.schemes?first.displayName}</h1>

<#else>

	<h1>Copy points</h1>

	<#assign popoverContent><#noescape>
		<ul>
			<#list command.schemes?sort_by("displayName") as scheme>
				<li>${scheme.displayName}</li>
			</#list>
		</ul>
	</#noescape></#assign>
	<p>
		You are copying these points to
		<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="top">
			<@fmt.p command.schemes?size "scheme" />
		</a>
	</p>

</#if>

<div class="fix-area">

	<form action="<@routes.attendance.manageAddPointsCopy command.department command.academicYear />" method="post" class="form-inline">
		<#list command.schemes as scheme>
			<input type="hidden" name="schemes" value="${scheme.id}" />
		</#list>

		<input type="hidden" name="returnTo" value="${returnTo}" />

		<@bs3form.form_group>
			<label style="margin-right: 12px;">Copy points from:</label>
			<label>Academic year</label>
			<select name="searchAcademicYear" class="form-control" style="margin-right: 12px;">
				<#list allAcademicYears as year>
					<option	value="${year.toString}"
						<#if searchAcademicYear??>
							<#if searchAcademicYear.toString == year.toString>
								selected
							</#if>
						<#elseif academicYear.toString == year.toString>
							selected
						</#if>
					>
						${year.toString}
					</option>
				</#list>
			</select>
		</@bs3form.form_group>

		<@bs3form.labelled_form_group labelText="Department">
			<select name="searchDepartment" class="form-control" style="margin-right: 12px;">
				<#list allDepartments as department>
					<option value="${department.code}" <#if searchDepartment?? && searchDepartment.name == department.name>selected</#if>>
					${department.name}
					</option>
				</#list>
			</select>
		</@bs3form.labelled_form_group>

		<@bs3form.form_group>
			<input type="submit" class="btn btn-primary" name="search" value="Display"/>
		</@bs3form.form_group>
	</form>

	<form action="<@routes.attendance.manageAddPointsCopy command.department command.academicYear />" method="post">
		<#list command.schemes as scheme>
			<input type="hidden" name="schemes" value="${scheme.id}" />
		</#list>

		<input type="hidden" name="returnTo" value="${returnTo}" />

		<#if allSchemes??>
			<input type="hidden" name="searchDepartment" value="${searchCommand.department.code}" />
			<input type="hidden" name="searchAcademicYear" value="${searchCommand.academicYear.startYear?c}" />

			<#if allSchemes?size == 0>
				<div class="alert alert-info">
					No schemes found
				</div>

				<button class="btn btn-default" type="submit" name="cancel">Cancel</button>
			<#else>

				<#if errors??>
					<div class="alert alert-danger">
						<#list errors.allErrors as error>
							<p><@spring.message code=error.code arguments=error.arguments/></p>
						</#list>
					</div>
				</#if>

				<p>Use the filters to choose which points to copy:</p>

				<div class="student-filter points-filter btn-group-group well well-sm">

					<button type="button" class="clear-all-filters btn btn-link">
						<span class="fa-stack">
							<i class="fa fa-filter fa-stack-1x"></i>
							<i class="fa fa-ban fa-stack-2x"></i>
						</span>
					</button>

					<#macro filter path placeholder currentFilter allItems validItems=allItems prefix="">
						<@spring.bind path=path>
							<div class="btn-group<#if currentFilter == placeholder> empty-filter</#if>">
								<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">
									<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
									<span class="caret"></span>
								</a>
								<div class="dropdown-menu filter-list">
									<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
									<ul>
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
												<li class="check-list-item checkbox" data-natural-sort="${item_index}">
													<label class="checkbox <#if !isValid>disabled</#if>">
														<#nested item isValid/>
													</label>
												</li>
											</#list>
										<#else>
											<li><small class="very-subtle" style="padding-left: 5px;">N/A for this department</small></li>
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

					<#function contains_by_field collection item field>
						<#list collection as c>
							<#if c[field] == item[field]>
								<#return true />
							</#if>
						</#list>
						<#return false />
					</#function>

					<#assign placeholder = "All schemes" />
					<#assign schemesBindPoint = "findCommand.findSchemes"/>
					<#assign schemesCollection = findCommand.findSchemes/>
					<#assign currentfilter><@current_filter_value schemesBindPoint placeholder; scheme>${scheme.shortDisplayName}</@current_filter_value></#assign>
					<@filter schemesBindPoint placeholder currentfilter allSchemes; scheme>
						<input type="checkbox" name="${status.expression}" value="${scheme.id}" data-short-value="${scheme.shortDisplayName}"
							${contains_by_field(schemesCollection, scheme "id")?string('checked','')}
						>
						<span title="${scheme.displayName}">${scheme.displayName}</span>
					</@filter>

					<#assign placeholder = "All types" />
					<#assign currentfilter><@current_filter_value "findCommand.types" placeholder; type>${type.description}</@current_filter_value></#assign>
					<@filter "findCommand.types" placeholder currentfilter allTypes; type>
						<input type="checkbox" name="${status.expression}" value="${type.dbValue}" data-short-value="${type.description}"
							${contains_by_field(findCommand.types, type "dbValue")?string('checked','')}
						>
						${type.description}
					</@filter>

					<div class="btn-group empty-filter">
						<a class="btn btn-default btn-xs">
							<span class="filter-short-values">
								<#if findCommand.restrictedStyle.dbValue == "week">
									Term week points only
								<#else>
									Calendar date points only
								</#if>
							</span>
						</a>
					</div>

					<div class="btn-group">
						<button class="btn btn-xs btn-primary search" type="submit" name="search">
							Filter
						</button>
					</div>

				</div>

				<#include "_displayfindpointresults.ftl" />

				<#if !findResult.termGroupedPoints?keys?has_content
					&& !findResult.monthGroupedPoints?keys?has_content>
					<div class="alert alert-info">
						No points found for the specified filter
					</div>

					<button class="btn btn-default" type="submit" name="cancel">Cancel</button>
				<#else>

					<div class="submit-buttons fix-footer save-row">
						<button class="btn btn-primary spinnable spinner-auto" type="submit" name="copy" data-loading-text="Copying&hellip;">
							Copy
						</button>
						<button class="btn btn-default" type="submit" name="cancel">Cancel</button>
					</div>

				</#if>

			</#if>

		</#if>
	</form>

</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();

		var prependClearLink = function($list) {
			if (!$list.find('input:checked').length) {
				$list.find('.clear-this-filter').remove();
			} else {
				if (!$list.find('.clear-this-filter').length) {
					$list.find('> ul').prepend(
						$('<li />').addClass('clear-this-filter').append(
							$('<button />').attr('type', 'button')
								.addClass('btn btn-link')
								.html('Clear selected items')
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

		$('.student-filter input').on('change', function() {
			// Load the new results
			var $checkbox = $(this);
			updateFilter($checkbox);
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
		});
	});
</script>

</#escape>