<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />

<#assign filterQuery = findCommand.serializeFilter />
<#assign returnTo = (info.requestedUri!"")?url />

<h1>Edit points</h1>

<#if newPoints == 0>
	<p>Which points do you want to edit?</p>
<#else>
	<div class="alert alert-info">
		<strong><@fmt.p newPoints "point" /></strong> ${actionCompleted!"edited"} on <strong><@fmt.p newPoints "scheme" /></strong>
	</div>
</#if>

<div class="fix-area">

	<form action="">

		<div class="fix-header points-filter student-filter btn-group-group well well-sm">

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
			<#assign currentfilter><@current_filter_value "findCommand.findSchemes" placeholder; scheme>${scheme.shortDisplayName}</@current_filter_value></#assign>
			<@filter "findCommand.findSchemes" placeholder currentfilter allSchemes; scheme>
				<input type="checkbox" name="${status.expression}" value="${scheme.id}" data-short-value="${scheme.shortDisplayName}"
					${contains_by_field(findCommand.findSchemes, scheme "id")?string('checked','')}
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

			<#assign placeholder = "All date formats" />
			<#assign currentfilter><@current_filter_value "findCommand.styles" placeholder; style>${style.description}</@current_filter_value></#assign>
			<@filter "findCommand.styles" placeholder currentfilter allStyles; style>
				<input type="checkbox" name="${status.expression}" value="${style.dbValue}" data-short-value="${style.description}"
					${contains_by_field(findCommand.styles, style "dbValue")?string('checked','')}
				>
				${style.description}
			</@filter>

			<div class="btn-group">
				<button class="btn btn-xs btn-primary search" type="submit">
					Filter
				</button>
			</div>

		</div>

		<#include "_findpointsresult.ftl" />
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
								.html('<i class="fa fa-ban"></i> Clear selected items')
								.on('click', function(e) {
									$list.find('input:checked').each(function() {
										var $checkbox = $(this);
										$checkbox.prop('checked', false);
										updateFilter($checkbox);
									});
								})
							).append($('<hr />'))
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