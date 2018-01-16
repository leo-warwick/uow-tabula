/**
 * Scripts used only by the small group teaching admin section.
 */
(function ($) { "use strict";

var exports = {};

exports.zebraStripeGroups = function($module) {
	$module.find('.group-info').filter(':visible:even').addClass('alt-row');
};

// take anything we've attached to "exports" and add it to the global "Groups"
// we use extend() to add to any existing variable rather than clobber it
window.Groups = jQuery.extend(window.Groups, exports);

$(function(){
	exports.fixHeaderFooter = $('.fix-area').fixHeaderFooter();
	window.Groups = jQuery.extend(window.Groups, exports);
	$('#action-submit').closest('form').on('click', '.update-only', function() {
		$('#action-submit').val('update');
		$('#action-submit').closest('form').find('[type=submit]').attr('disabled', true);
		$(this).attr('disabled', false);
	});

    // Zebra striping on lists of modules/groups
    $('.module-info').each(function(i, module) {
        exports.zebraStripeGroups($(module));
    });

    $('.module-info.empty').css('opacity',0.66)
        .find('.module-info-contents').hide().end()
        .click(function(){
            $(this).css('opacity',1)
                .find('.module-info-contents').show().end();
        })
        .hide();

    $('.dept-show').click(function(event){
		event.preventDefault();
    	var hideButton = $(this).find("a");

        $('.striped-section.empty').toggle('fast', function() {
        	if($('.module-info.empty').is(":visible")) {
        		hideButton.html('Hide');
        		hideButton.attr("data-original-title", hideButton.attr("data-title-hide"));

			} else {
        		hideButton.html('Show');
        		hideButton.attr("data-original-title", hideButton.attr("data-title-show"));
        	}
        });

    });

    $('.show-archived-small-groups').click(function(e){
        e.preventDefault();
        $(e.target).hide().closest('.striped-section').find('.item-info.archived').show();
    });

    // enable/disable the "sign up" buttons on the student groups homepage
    $('#student-groups-view .sign-up-button').addClass('disabled use-tooltip').prop('disabled',true).attr('title','Please select a group');
    $('#student-groups-view input.group-selection-radio').change(function(){
		$(this).closest('.item-info').find('.sign-up-button').removeClass('disabled use-tooltip').prop('disabled',false).attr('title','');
	});
});


// modals use ajax to retrieve their contents
$(function() {
	$('body').on('click', 'a[data-toggle=modal]', function(e){
		e.preventDefault();
		var $this = $(this);
		var $target = $($this.attr('data-target'));
		var url = $this.attr('href');
		if (url != undefined) {
			$target.load(url, function(){
				$target.find('form.double-submit-protection').tabulaSubmitOnce();
			});
		}
	});

    $("#modal-container").on("click","input[type='submit']", function(e){
        e.preventDefault();
        var $this = $(this);
        var $form = $this.closest("form").trigger('tabula.ajaxSubmit');
        $form.removeClass('dirty');
        var updateTargetId = $this.data("update-target");

        var randomNumber = Math.floor(Math.random() * 10000000);

        jQuery.post($form.attr('action') + "?rand=" + randomNumber, $form.serialize(), function(data){
            $("#modal-container ").modal('hide');
            if (updateTargetId){
               $(updateTargetId).html(data);
            }else{
	            window.location.reload();
	        }
        });
    });
});

// Week selector and location picker
$(function() {
	$('table.week-selector').each(function() {
		var $table = $(this);

		var updateCell = function($cell, value) {
			var groupRunningText = 'Group running on ';
			if (value) {
				$cell.attr('data-original-title', groupRunningText + $cell.attr('data-original-title'));
			} else {
				$cell.attr('data-original-title', $cell.attr('data-original-title').replace(groupRunningText, ''));
			}
		};

		$table.find('tbody tr').each(function(){
			$(this).bigList({
				'onChange' : function(){
					updateCell($(this).closest('td'), $(this).is(':checked'));
				}
			});
		});

		$table.find('.show-vacations').each(function() {
			var $checkbox = $(this);

			if ($table.find('tr.vacation td').find(':checked').length) {
				$checkbox.prop('checked', true);
			}

			var updateDisplay = function() {
				if ($checkbox.is(':checked')) {
					$table.find('tr.vacation').show();
				} else {
					$table.find('tr.vacation').hide();
				}
			};
			updateDisplay();

			$checkbox.on('change', updateDisplay);
		});
	});

	var $location = $('#location, #defaultLocation');
	var $locationId = $('#locationId, #defaultLocationId');
	var $namedLocationAlert = $('#namedLocationAlert');
	var locationValue = $location.val();
	var locationIdValue = $locationId.val();
	if ($('#useNamedLocation.errors').length > 0 || (locationValue != undefined && locationValue.length > 0 && locationIdValue != undefined && locationIdValue.length === 0)) {
		$namedLocationAlert.show();
	}

	$('input#location, input#defaultLocation')
		.on('change', function () {
			var $this = $(this);

			if ($this.val() !== '' && $this.data('location-name') === $this.val()) {
				// Value hasn't changed since we last set the location ID
				return;
			}

			if ($this.data('lid') === undefined || $this.data('lid').length === 0) {
				$this.data('location-name', '');
				$this.closest('.form-group').find('input[type="hidden"]').val('');
				$namedLocationAlert.toggle($this.val().length > 0);
				return;
			}

			$namedLocationAlert.hide();

			$this.closest('.form-group').find('input[type="hidden"]').val($this.data('lid'));
			$this.data('lid', '').data('location-name', $this.val());
		})
		.locationPicker();

	var $locationAliasFormGroup = $('.location-alias-form-group');
	var $showLocationAlias = $('#showLocationAlias');
	var $removeLocationAlias = $('#removeLocationAlias');
	var inputValue = $locationAliasFormGroup.find('input').val();
	if (inputValue != undefined && inputValue.length === 0) {
		$locationAliasFormGroup.hide();
	} else {
		$showLocationAlias.parent().hide();
	}

	$showLocationAlias.on('click', function (e) {
		e.preventDefault();
		$showLocationAlias.parent().hide();
		$locationAliasFormGroup.show().find('input').focus();
	});

	$removeLocationAlias.on('click', function (e) {
		e.preventDefault();
		$showLocationAlias.parent().show();
		$locationAliasFormGroup.hide();
		$locationAliasFormGroup.find('input').val('');
	});
});

// Re-usable small groups
$(function() {
	if ($('.add-student-to-set').length > 0) {
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
				, $section = $th.closest('.striped-section');

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

			if ($section.data('submitparam').length > 0) {
				$form.append($('<input/>').attr({
					'type': 'hidden',
					'name': $section.data('submitparam'),
					'value': true
				}));
			}
			$form.submit();
		});

		$('.pagination').on('click', 'a', function() {
			var $this = $(this), $form = $this.closest('form'), $section = $this.closest('.striped-section');
			if ($this.data('page').toString.length > 0) {
				$form.find('input[name="page"]').remove().end()
					.append($('<input/>').attr({
						'type': 'hidden',
						'name': 'page',
						'value': $this.data('page')
					})
				);
			}
			if ($section.data('submitparam').length > 0) {
				$form.find('input[name="' + $section.data('submitparam') + '"]').remove().end()
					.append($('<input/>').attr({
						'type': 'hidden',
						'name': $section.data('submitparam'),
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
								.html('Clear selected items')
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
			// Add in route search
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
			var $filterList = $el.closest(".student-filter, .small-groups-filter");

			if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
				$('.clear-all-filters').attr("disabled", "disabled");
			} else {
				$('.clear-all-filters').removeAttr("disabled");
			}
		};

		var updateSearchButton = function($el) {
			var $filter = $el.closest('.student-filter, .small-groups-filter');
			if ($filter.find('input:checked').length > 0) {
				$filter.find('button.search').attr('disabled', false);
			} else {
				$filter.find('button.search').attr('disabled', true);
			}
		};

		$('.student-filter input, .small-groups-filter input').on('change', function() {
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
	}
});

}(jQuery));