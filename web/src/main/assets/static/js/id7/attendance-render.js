/**
 * Scripts used only by the attendance monitoring section.
 */
(function ($) { "use strict";

var exports = {};

$(function(){

	// Scripts for Add students
	(function(){
		$('.mass-add-users')
			.find('textarea').on('keyup', function(){
				var $this = $(this), $addButton = $this.closest('.mass-add-users').find('input.btn.add-students');
				if ($this.val().length === 0) {
					$addButton.addClass('disabled');
				} else {
					$addButton.removeClass('disabled');
				}
			}).end()
		;

		var updateButtons = function(){
			var checkboxes = $('.manually-added tbody input')
				, checkedBoxes = checkboxes.filter(function(){ return $(this).is(':checked'); });
			if (checkedBoxes.length == 0) {
				$('.manually-added summary input.btn-warning').attr('disabled', true);
			} else {
				$('.manually-added summary input.btn-warning').attr('disabled', false);
			}
		};
		updateButtons();
		$('.manually-added')
			.find('.for-check-all').append(
				$('<input/>').addClass('check-all use-tooltip').attr({
					type: 'checkbox',
					title: 'Select all/none'
				}).addClass('collection-check-all')
			).end().bigList({
				onChange: updateButtons
			});

		if ($('.add-student-to-scheme').length > 0) {
			$('.fix-area').fixHeaderFooter();

			$('.tablesorter').find('th.sortable').addClass('header').on('click', function() {
				var $th = $(this)
					, sortDescending = function() {
						$('#sortOrder').val('desc(' + $th.data('field') + ')');
						$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
						$th.addClass('headerSortUp');
					}, sortAscending = function() {
						$('#sortOrder').val('asc(' + $th.data('field') + ')');
						$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
						$th.addClass('headerSortDown');
					}, $form = $th.closest('form')
					, $container = $th.closest('div.striped-section');

				if ($th.hasClass('headerSortUp')) {
					sortAscending();
				} else if ($th.hasClass('headerSortDown')) {
					sortDescending();
				} else {
					// not currently sorted on this column, default sort depends on column
					if ($th.hasClass('unrecorded-col') || $th.hasClass('missed-col')) {
						sortDescending();
					} else {
						sortAscending();
					}
				}

				if ($container.data('submitparam').length > 0) {
					$form.append($('<input/>').attr({
						'type': 'hidden',
						'name': $container.data('submitparam'),
						'value': true
					}));
				}
				$form.submit();
			});

			$('.pagination').on('click', 'a', function() {
				var $this = $(this), $form = $this.closest('form'), $container = $this.closest('div.striped-section');
				if ($this.data('page').toString.length > 0) {
					$form.find('input[name="page"]').remove().end()
						.append($('<input/>').attr({
							'type': 'hidden',
							'name': 'page',
							'value': $this.data('page')
						})
					);
				}
				if ($container.data('submitparam').length > 0) {
					$form.find('input[name="' + $container.data('submitparam') + '"]').remove().end()
						.append($('<input/>').attr({
							'type': 'hidden',
							'name': $container.data('submitparam'),
							'value': true
						})
					);
				}
				$form.submit();
			});

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
										.on('click', function() {
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
				var shortValues = $list.find(':checked').map(function() {
					return $(this).data('short-value');
				}).get();
				var $fsv = $el.closest('.btn-group').find('.filter-short-values');
				if (shortValues.length) {
					$el.closest('.btn-group').removeClass('empty-filter');
					$fsv.html($fsv.data("prefix") + shortValues.join(', '));
				} else {
					$el.closest('.btn-group').addClass('empty-filter');
					$fsv.html($fsv.data('placeholder'));
				}

				updateSearchButton($el);
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

			var updateSearchButton = function($el) {
				var $filter = $el.closest('.student-filter');
				if ($filter.find('input:checked').length > 0) {
					$filter.find('button.search').attr('disabled', false);
				} else {
					$filter.find('button.search').attr('disabled', true);
				}
			};

			$('.student-filter input').on('change', function() {
				// Load the new results
				var $input = $(this);
				if ($input.is('.prevent-reload')) return;
				updateFilter($input);
				$('.student-filter-change').val(true);
			});

			// Re-order elements inside the dropdown when opened
			$('.filter-list').closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function() {
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
					$('<li/>').addClass('check-list-item').append(
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
		}
	})();

	// Add points
	(function(){
		var updateButtons = function(){
			var weekCheckboxes = $('.add-points-to-schemes table.week tbody input')
				, weekCheckedBoxes = weekCheckboxes.filter(function(){ return $(this).is(':checked'); })
				, dateCheckboxes = $('.add-points-to-schemes table.date tbody input')
				, dateCheckedBoxes = dateCheckboxes.filter(function(){ return $(this).is(':checked'); })
				;
			if (weekCheckedBoxes.length === 0 && dateCheckedBoxes.length === 0) {
				$('.add-points-to-schemes p button').attr('disabled', true);
				$('.add-points-to-schemes span.alert').hide();
			} else if (weekCheckedBoxes.length > 0 && dateCheckedBoxes.length > 0) {
				$('.add-points-to-schemes p button').attr('disabled', true);
				$('.add-points-to-schemes span.alert').show();
			} else {
				$('.add-points-to-schemes p button').attr('disabled', false);
				$('.add-points-to-schemes span.alert').hide();
			}
		};
		updateButtons();
		$('.add-points-to-schemes')
			.find('.for-check-all').append(
				$('<input/>').addClass('check-all use-tooltip').attr({
					type: 'checkbox',
					title: 'Select all/none'
				}).on('click', function(){
					$(this).closest('table').find('tbody input').prop('checked', this.checked);
					updateButtons();
				})
			).end()
			.find('tbody tr').on('click', function(){
				var $input = $(this).find('input');
				$input.prop('checked', !$input.is(':checked'));
				updateButtons();
			}).end()
			.find('table tbody input').on('click', function(e){
				e.stopPropagation();
				updateButtons();
			}).end()
			.find('button').on('click', function(){
				var $this = $(this), $form = $this.closest('form');
				$form.attr('action', $this.data('href')).submit();
			})
		;
	})();
});

window.Attendance = jQuery.extend(window.Attendance, exports);

}(jQuery));