/**
* Scripts used only by the attendance monitoring section.
*/
(function ($) { "use strict";

var exports = {};

exports.Manage = {};

exports.scrollablePointsTableSetup = function() {
    $('.scrollable-points-table .middle .sb-wide-table-wrapper').parent().addClass('scrollable');
    $('.scrollable-points-table .middle .scrollable').addClass('rightShadow').find('.sb-wide-table-wrapper').on('scroll', function(){
       var $this = $(this);
       if($this.scrollLeft() > 0) {
           $this.parent(':not(.leftShadow)').addClass('leftShadow');
       } else {
           $this.parent().removeClass('leftShadow');
       }
       if($this.scrollLeft() == 0 || $this.get(0).scrollWidth - $this.scrollLeft() != $this.outerWidth()) {
           $this.parent(':not(.rightShadow)').addClass('rightShadow');
       } else {
           $this.parent().removeClass('rightShadow');
       }
    });
};

exports.tableSortMatching = function(tableArray) {
    var matchSorting = function($sourceTable, targetTables){
        var $sourceRows = $sourceTable.find('tbody tr');
        $.each(targetTables, function(i, $table){
            var $tbody = $table.find('tbody');
            var oldRows = $tbody.find('tr').detach();
            $.each($sourceRows, function(j, row){
                var $sourceRow = $(row);
                oldRows.filter(function(){ return $(this).data('sortId') == $sourceRow.data('sortId'); }).appendTo($tbody);
            });
        });
    };

    if (tableArray.length < 2)
        return;

    $.each(tableArray, function(i){
        var otherTables = tableArray.slice();
        otherTables.splice(i, 1);
        this.on('sortEnd', function(){
            matchSorting($(this), otherTables);
        }).find('tbody tr').each(function(i){ $(this).data('sortId', i); });
    });
};

exports.bindModulePickers = function(){
	$('.module-search-query')
		.closest('.module-choice')
			.find('.modules-list').on('click', 'button.btn.btn-danger', function(){
				$(this).closest('li').remove();
			}).end()
		.end()
		.each(function(){
			var $this = $(this),
				$parent = $this.closest('.module-choice'),
				$hiddenInput = $parent.find('input[type=hidden]:first');
			$this.on('change', function(){
				if ($(this).data('moduleid') === undefined || $(this).data('moduleid').length === 0)
					return;

				$this.closest('.module-choice').find('input.specific[name=isAnySmallGroupEventModules]').attr('checked', true);
				var icon = $('<i/>').addClass('icon-fixed-width');
				if ($this.hasClass('smallGroup') && $this.data('hasgroups') != true) {
					icon.addClass('icon-exclamation-sign').attr({
						'title':'This module has no small groups set up in Tabula'
					});
				}
				if ($this.hasClass('assignment') && $this.data('hasassignments') != true) {
					icon.addClass('icon-exclamation-sign').attr({
						'title':'This module has no assignments set up in Tabula'
					});
				}
				$this.closest('.module-choice').find('.modules-list ol').append(
					$('<li/>').append(
						$('<input/>').attr({
							'type':'hidden',
							'name':$hiddenInput.attr('name').replace('_',''),
							'value':$this.data('moduleid')
						})
					).append(
						icon
					).append(
						$('<span/>').attr('title', $this.val()).html($this.val())
					).append(
						$('<button/>').addClass('btn btn-danger btn-mini').append(
							$('<i/>').addClass('icon-remove')
						)
					)
				);
				$this.data('moduleid','').val('');
			});
	});
	$('.pointTypeOption.smallGroup .module-search-query').modulePicker({
		checkGroups: true
	});
	$('.pointTypeOption.assignmentSubmission .module-search-query').modulePicker({
		checkAssignments: true
	});
};

exports.bindAssignmentPickers = function(){
	$('.assignment-search-query')
		.closest('.assignment-choice')
			.find('.assignments-list').on('click', 'button.btn.btn-danger', function(){
				$(this).closest('li').remove();
			}).end()
		.end()
		.each(function(){
			var $this = $(this),
				$parent = $this.closest('.assignment-choice'),
				$hiddenInput = $parent.find('input[type=hidden]:first');
			$this.on('change', function(){
				if ($(this).data('assignmentid') === undefined || $(this).data('assignmentid').length === 0)
					return;

				$this.closest('.assignment-choice').find('.assignments-list ol').append(
					$('<li/>').append(
						$('<input/>').attr({
							'type':'hidden',
							'name':$hiddenInput.attr('name').replace('_',''),
							'value':$this.data('assignmentid')
						})
					).append(
						$('<span/>').attr('title', $this.val()).html($this.val())
					).append(
						$('<button/>').addClass('btn btn-danger btn-mini').append(
							$('<i/>').addClass('icon-remove')
						)
					)
				);
				$this.data('assignmentid','').val('');
			});
		});

	$('.pointTypeOption.assignmentSubmission .assignment-search-query').assignmentPicker({});
};

$(function(){

	// SCRIPTS FOR MANAGING MONITORING POINTS

	if ($('#chooseCreateType').length > 0 && setsByRouteByAcademicYear) {
		var $form = $('#chooseCreateType'),
		    academicYearSelect = $form.find('select.academicYear'),
		    routeSelect = $form.find('select.route'),
		    yearSelect = $form.find('select.copy[name="existingSet"]');

		$form.find('input.create').on('change', function(){
			if ($(this).val() === 'copy') {
				$form.find('button').html('Copy').prop('disabled', true);
				$form.find('.existingSetOptions').css('visibility','hidden');
				$form.find('select.template[name="existingSet"]').prop('disabled', true);
				academicYearSelect.add(routeSelect).add(yearSelect).prop('disabled', false).css('visibility','hidden');
				academicYearSelect.find('option:first').prop('selected', true);
				academicYearSelect.css('visibility','visible');
			} else if ($(this).val() === 'template') {
				$form.find('button').html('Create').prop('disabled', false);
				$form.find('.existingSetOptions').css('visibility','visible');
                $form.find('select.template[name="existingSet"]').prop('disabled', false);
                academicYearSelect.add(routeSelect).add(yearSelect).prop('disabled', true).css('visibility','hidden');
			} else {
				$form.find('button').html('Create').prop('disabled', false);
				$form.find('.existingSetOptions').css('visibility','hidden');
				$form.find('select.template[name="existingSet"]').prop('disabled', true)
				academicYearSelect.add(routeSelect).add(yearSelect).prop('disabled', true).css('visibility','hidden');
			}
		});

		$form.on('submit', function(){
			var createType = $form.find('input.create:checked').val();
			if (createType === 'copy') {
				$form.find('select[name="existingSet"]').not('.copy').prop('disabled', true);
			} else if (createType === 'template') {
				$form.find('select[name="existingSet"]').not('.template').prop('disabled', true);
			} else {
				$form.find('select[name="existingSet"]').prop('disabled', true);
			}
		});

		academicYearSelect.on('change', function(){
			routeSelect.find('option:gt(0)').remove()
				.end().css('visibility','visible');
			yearSelect.find('option:gt(0)').remove()
				.end().css('visibility','hidden');
			$.each(academicYearSelect.find('option:selected').data('routes'), function(i, route){
				routeSelect.append(
					$('<option/>').data('sets', route.sets).html(route.code.toUpperCase() + ' ' + route.name)
				);
			});
		});

		routeSelect.on('change', function(){
			yearSelect.find('option:gt(0)').remove()
				.end().find('option:first').prop('selected', true)
				.end().css('visibility','visible').change();
			$.each(routeSelect.find('option:selected').data('sets'), function(i, set){
				yearSelect.append(
					$('<option/>').val(set.id).html(set.year)
				);
			});
		});

		yearSelect.on('change', function(){
			if (yearSelect.val() && yearSelect.val().length > 0) {
				$form.find('button').prop('disabled', false);
			} else {
				$form.find('button').prop('disabled', true);
			}
		});

		// Update Preview button to preview the currently selected template.
		$form.find('select.template[name=template]').on('change', function() {
			var $button = $('.monitoring-point-preview-button'),
			    newHref = $button.data('hreftemplate').replace('_TEMPLATE_ID_', this.value);
			$button.attr('href', newHref);
		}).trigger('change');

		// Trigger the above change behaviour for the initial value
		$form.find('input.create:checked').trigger('change');

		if (academicYearSelect.length > 0) {
			for (var academicYear in setsByRouteByAcademicYear) {
				academicYearSelect.append(
					$('<option/>').data('routes', setsByRouteByAcademicYear[academicYear]).html(academicYear)
				);
			}
		}
	}

	var setupCollapsible = function($this, $target){
		$this.css('cursor', 'pointer').on('click', function(){
			var $chevron = $this.find('i.icon-fixed-width');
			if ($this.hasClass('expanded')) {
				$chevron.removeClass('icon-chevron-down').addClass('icon-chevron-right');
				$this.removeClass('expanded');
				$target.hide();
			} else {
				$chevron.removeClass('icon-chevron-right').addClass('icon-chevron-down');
				$this.addClass('expanded');
				$target.show();
			}
		});
		if ($this.hasClass('expanded')) {
			$target.show();
		} else {
			$target.hide();
		}
	};

	var updateRouteCounter = function(){
		var total = $('.routeAndYearPicker').find('div.scroller tr').filter(function(){
			return $(this).find('input:checked').length > 0;
		}).length;
		if (total === 0) {
			$('.routeAndYearPicker').find('span.routes-count').html('are no routes');
		} else if (total === 1) {
			$('.routeAndYearPicker').find('span.routes-count').html('is 1 route');
		} else {
         	$('.routeAndYearPicker').find('span.routes-count').html('are ' + total + ' routes');
         }
	};

	$('.striped-section.routes .collapsible').each(function(){
		var $this = $(this), $target = $this.closest('.item-info').find('.collapsible-target');
		setupCollapsible($this, $target);
	});

	$('.routeAndYearPicker').find('.collapsible').each(function(){
		var $this = $(this), $target = $this.parent().find('.collapsible-target');
		if ($target.find('input:checked').length > 0) {
			$this.addClass('expanded');
			updateRouteCounter();
		}
		setupCollapsible($this, $target);
	}).end().find('tr.years th:gt(0)').each(function(){
		var $this = $(this), oldHtml = $this.html();
		$this.empty().append(
			$('<input/>').attr({
				'type':'checkbox',
				'title':'Select all/none'
			}).on('click', function(){
				var checked = $(this).prop('checked');
				$this.closest('div').find('div.scroller tr').each(function(){
					$(this).find('td.year_' + $this.data('year') + ' input').not(':disabled').prop('checked', checked);
				});
				updateRouteCounter();
			})
		).append(oldHtml);
	}).end().find('td input:not(:disabled)').on('click', function(){
		var $this = $(this);
		$this.closest('div.collapsible-target').find('th.year_' + $this.data('year') + ' input').prop('checked', false);
		updateRouteCounter();
	});

	var pointsChanged = false;

	var bindPointButtons = function(){
    	$('.modify-monitoring-points a.new-point, .modify-monitoring-points a.edit-point, .modify-monitoring-points a.delete-point')
    		.off('click').not('.disabled').on('click', function(e){

    		e.preventDefault();
    		var formLoad = function(data){
    			var $m = $('#modal');
    			$m.html(data);
    			if ($m.find('.monitoring-points').length > 0) {
    				$('.modify-monitoring-points .monitoring-points').replaceWith($m.find('.monitoring-points'))
    				$m.modal("hide");
    				bindPointButtons();
    				pointsChanged = true;
    				return;
    			}
    			var $f = $m.find('form');
    			$f.on('submit', function(event){
    				event.preventDefault();
    			});
    			$m.find('.modal-footer button[type=submit]').on('click', function(){
    				$(this).button('loading');
    				$.post($f.attr('action'), $f.serialize(), function(data){
    					$(this).button('reset');
    					formLoad(data);
    				})
    			});
    			$m.off('shown').on('shown', function(e){
					if ($(e.target).is('#modal')) {
    					$f.find('input[name="name"]').focus();
					}
    			}).modal("show");
    		};
    		if ($('form#addMonitoringPointSet').length > 0) {
    			$.post($(this).attr('href'), $('form#addMonitoringPointSet').serialize(), formLoad);
    		} else {
    			$.get($(this).attr('href'), formLoad);
    		}
    	});
    };
    bindPointButtons();

    $('#addMonitoringPointSet').on('submit', function(){
    	pointsChanged = false;
    })

    if ($('#addMonitoringPointSet').length > 0) {
    	$(window).bind('beforeunload', function(){
    		if (pointsChanged) {
    			return 'You have made changes to the monitoring points. If you continue they will be lost.'
    		}
		});
	}

	// END SCRIPTS FOR MANAGING MONITORING POINTS

});

// TAB-1974 scripts

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
		;

		if ($('.add-student-to-scheme').length > 0) {
			$('.fix-area').fixHeaderFooter();
			$('details').on('open.details close.details', function() {
				setTimeout(function() {
					$(window).trigger('resize');
				}, 10);
			});

			var allStudentsAjax;
			$('details.all-students').on('open.details', function(){
				var $this = $(this);
				if ($this.find('div.students').length === 0 && allStudentsAjax === undefined) {
					$this.find('.loading').show();
					allStudentsAjax = $.ajax($this.data('href'), {
						type: 'POST',
						data: $this.closest('form').serialize(),
						success: function(result) {
							$this.find('.loading').hide();
							$this.append(result);
							// Enable any freshly loaded popovers
							jQuery('.use-popover').tabulaPopover({
								trigger: 'click',
								container: 'body'
							});
						}
					})
				}
			});

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
					, $details = $th.closest('details');

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

				if ($details.data('submitparam').length > 0) {
					$form.append($('<input/>').attr({
						'type': 'hidden',
						'name': $details.data('submitparam'),
						'value': true
					}));
				}
				$form.submit();
			});

			$('.pagination').on('click', 'a', function() {
				var $this = $(this), $form = $this.closest('form'), $details = $this.closest('details');
				if ($this.data('page').toString.length > 0) {
					$form.find('input[name="page"]').remove().end()
						.append($('<input/>').attr({
							'type': 'hidden',
							'name': 'page',
							'value': $this.data('page')
						})
					);
				}
				if ($details.data('submitparam').length > 0) {
					$form.find('input[name="' + $details.data('submitparam') + '"]').remove().end()
						.append($('<input/>').attr({
							'type': 'hidden',
							'name': $details.data('submitparam'),
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
										.html('<i class="icon-ban-circle"></i> Clear selected items')
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

