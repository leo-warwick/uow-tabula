(function ($) {
	'use strict';

$.fn.enableFilters = function(options) {

	options = options || {};

	var resultSelector = options.resultSelector || '.filter-results';

	this.each(function() {
		var $this = $(this);
		var $form = $this.find('form');
		var $results = $(resultSelector);
		var $clearAll = $this.find('.clear-all');

		// bind event handlers
		$this.on('change', function(e) {
			var $checkbox = $(e.target);
			doRequest();
			updateFilterText($checkbox);
			updateRelatedFilters($checkbox);
		});

		$this.find('.module-picker').on('change', function(){
			var $picker = $(this);
			if ($picker.data('modulecode') === undefined || $picker.data('modulecode').length === 0)
				return;

			updateFilterFromPicker($picker, 'modules', $picker.data('modulecode'), $picker.data('modulecode').toUpperCase());

			$picker.data('modulecode','').val('');
		});

		$clearAll.on('click', function(){
			$this.find('input:checked').each(function() {
				var $checkbox = $(this);
				$checkbox.prop('checked', false);
				updateFilterText($checkbox);
				updateRelatedFilters($checkbox);
			});
			doRequest();
		});
		toggleClearAll();

		// Re-order elements inside the dropdown when opened
		$('.filter-list', $this).closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function() {
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

		$('input:checked').each(function(index, checkbox){
			updateFilterText($(checkbox));
		});

		$('input[data-related-filter]:checked').each(function(index, checkbox){
			updateRelatedFilters($(checkbox));
		});

		function doRequest() {
			// update the url with the new filter values
			if (typeof history.pushState !== 'undefined') {
				history.pushState(null, document.title, $form.attr('action') + '?' + $form.serialize());
			}

			// abort any currently running requests
			if ($form.data('request')) {
				$form.data('request').abort();
				$form.data('request', null);
			}

			// grey out results while loading
			$results.addClass('loading');

			$form.data('request', $.post($form.attr('action'), $form.serialize(), function(data) {
				$results.html(data);
				$form.data('request', null);
				$results.removeClass('loading');
			}));
			toggleClearAll();
		}

		function toggleClearAll() {
			$clearAll.prop('disabled', ($this.find('input:checked').size() === 0));
		}

		function updateFilterText($checkbox) {
			var $filter = $checkbox.closest('.filter');
			var shortValues = $filter.find('input:checked').map(function() { return $(this).data('short-value'); }).get();
			var $fsv = $filter.find('.filter-short-values');
			if (shortValues.length) {
				$filter.removeClass('empty');
				$fsv.html($fsv.data("prefix") + shortValues.join(', '));
			} else {
				$filter.addClass('empty');
				$fsv.html($fsv.data('placeholder'));
			}
		}
		
		// hides any invalid options in related filters
		function updateRelatedFilters($checkbox) {
			var related = $checkbox.data('related-filter');
			if(related) {
				var name = $checkbox.attr("name");
				var values = $('input[name='+name+']:checked').map(function(){return $(this).val()});
				var $relatedFilter = $('#'+related+'-filter');
				if(values.length > 0){
					// hide all filters
					$relatedFilter.find('li:not(:has(>.module-search))').hide();
					// show filters that match the value of this filter
					$.each(values, function(index, value){
						$relatedFilter.find('input[data-related-value="'+value+'"]').closest('li').show();
					});
				} else {
					$relatedFilter.find('li').show();
				}
			}
		}

		// add a clear list when at least one option is checked
		function prependClearLink($list) {
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
											updateFilterText($checkbox);
											updateRelatedFilters($checkbox);
										});
										doRequest();
									})
							)
							.append($('<hr />'))
					);
				}
			}
		}

		function updateFilterFromPicker($picker, name, value, shortValue) {
			if (value === undefined || value.length === 0)
				return;

			shortValue = shortValue || value;

			var $ul = $picker.closest('ul');

			var $li = $ul.find('input[value="' + value + '"]').closest('li');
			var $checkbox;
			if ($li.length) {
				$checkbox = $li.find('input').prop('checked', true);
				if ($ul.find('li.check-list-item:first').find('input').val() !== value) {
					$li.insertBefore($ul.find('li.check-list-item:first'));
				}
			} else {
				$checkbox = $('<input/>').attr({
					'type':'checkbox',
					'name':name,
					'value':value,
					'checked':true
				}).data('short-value', shortValue);

				$('<li/>').addClass('check-list-item').append(
					$('<label/>').addClass('checkbox').append($checkbox).append($picker.val())
				).insertBefore($ul.find('li.check-list-item:first'));
			}

			doRequest();
			updateFilterText($checkbox);
		}
	});
};

$(function(){
	$('.filters').enableFilters();
});

})(jQuery);