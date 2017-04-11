jQuery(function($){
	if ($('.allocate-associations').length === 0)
		return;


	var $fetchForm = $('form.fetch'), $studentFilter = $('#command').find('.student-filter');
	var EntityTable = function($table){
		var rowMap = {}, $removeButton = $table.closest('div').find('button.remove-all');

		$table.find('tr[data-forentity]').each(function(){
			var $this = $(this), entityId = $this.data('forentity');
			if (rowMap[entityId] === undefined) {
				rowMap[entityId] = [];
			}
			rowMap[entityId].push($this);
			$this.detach();
		});

		$table.on('click', '.btn-default', function(){
			var $this = $(this), $expandedInput = $this.closest('td').find('input'), $row = $this.closest('tr'), entityId = $row.data('entity');
			if ($row.hasClass('expanded')) {
				$table.find('tr[data-forentity="' + entityId + '"]').detach();
				$row.removeClass('expanded');
				$expandedInput.val('false');
			} else {
				$row.after(rowMap[entityId]);
				$row.addClass('expanded', true);
				$expandedInput.val('true');
			}
		});
		$table.find('tr.expanded').each(function(){
			$(this).after(rowMap[$(this).data('entity')]);
		});

		$table.sortableTable({
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.data('sortby')) {
					return $el.data('sortby');
				} else {
					return $el.text().trim();
				}
			}
		});
		$table.on('sortStart', function(){
			$table.find('tr[data-forentity]').detach();
		}).on('sortEnd', function(){
			$table.find('tr').each(function(){
				var $row = $(this), entityId = $row.data('entity');
				if ($row.hasClass('expanded')) {
					$row.after(rowMap[entityId]);
				}
			});
		});

		$table.on('click tabula.selectDeselectCheckboxes.toggle', 'input', function(){
			if ($table.find('input:checked').length === 0) {
				$removeButton.prop({
					'disabled': true,
					'title': 'You need to select some personal tutors from which to remove students'
				});
			} else {
				$removeButton.prop({
					'disabled': false,
					'title': 'All students will be removed from selected personal tutors'
				});
			}
		});
		$removeButton.prop('disabled', true);

		this.getTable = function() { return $table; }
	};

	var entityTable = new EntityTable($('.entities table'));

	var $studentTable = $('.students table'),
		$distributeSelectedButton = $studentTable.closest('div').find('button.distribute-selected'),
		$distributeAllButton = $studentTable.closest('div').find('button.distribute-all');

	var checkDistributeButtons = function(){
		var studentsChecked = $studentTable.find('input:checked').length > 0, entitiesChecked = entityTable.getTable().find('input:checked').length > 0;
		if (!entitiesChecked) {
			$distributeSelectedButton.add($distributeAllButton).prop({
				'disabled': true,
				'title': 'You need to select some students and personal tutors to allocate'
			}).removeClass('btn-primary');
		} else {
			$distributeAllButton.prop({
				'disabled': false,
				'title': 'All students will be equally distributed between selected personal tutors'
			}).addClass('btn-primary');
			if (studentsChecked) {
				$distributeSelectedButton.prop({
					'disabled': false,
					'title': 'Selected students will be equally distributed between selected personal tutors'
				}).addClass('btn-primary');
			} else {
				$distributeSelectedButton.prop({
					'disabled': true,
					'title': 'You need to select some students and personal tutors to allocate'
				}).removeClass('btn-primary');
			}
		}
	};
	$studentTable.sortableTable().on('click tabula.selectDeselectCheckboxes.toggle', 'input', checkDistributeButtons);
	entityTable.getTable().on('click tabula.selectDeselectCheckboxes.toggle', 'input', checkDistributeButtons);
	checkDistributeButtons();

	var $studentQuery = $('input[name=query]').on('keypress', function(e){
		if (e.which === 13) {
			$(this).closest('form').submit();
		}
	}).attr('autocomplete', 'off');
	var $typeahead = $studentQuery.bootstrap3Typeahead({
		source: function(query, process){
			// Abort any existing search
			if (self.currentSearch) {
				self.currentSearch.abort();
				self.currentSearch = null;
			}
			self.currentSearch = $.ajax({
				url: $fetchForm.attr('action'),
				data: $fetchForm.serialize(),
				success: function(data) {
					process(data.unallocated)
				}
			});
		},
		item: '<li class="flexi-picker-result"><a href="#"><span class="name"></span> (<span class="universityId"></span>)</a></li>'
	}).data('typeahead');
	$typeahead.sorter = function(items) { return items; };
	$typeahead.matcher = function() { return true; };
	$typeahead.render = function (items) {
		var that = this;
		items = $(items).map(function (i, item) {
			if (item != undefined) {
				i = $(that.options.item);
				i.find('span.name').html(that.highlighter(item.firstName + ' ' + item.lastName));
				i.find('span.universityId').html(that.highlighter(item.universityId));
				return i[0];
			} else {
				// no idea what's happened here. Return an empty item.
				return $(that.options.item)[0];
			}
		});
		items.first().addClass('active');
		this.$menu.html(items);
		return this;
	};
	$typeahead.show = function () {
		var pos = $.extend({}, this.$element.offset(), {
			height: this.$element[0].offsetHeight
		});

		this.$menu.appendTo($('body')).show().css({
			top: pos.top + pos.height, left: pos.left
		});

		this.shown = true;
		return this;
	};
	$typeahead.updater = function() {
		return this.$menu.find('.active .universityId').text();
	};
	var oldSelect = $typeahead.select;
	$typeahead.select = function () {
		oldSelect.call($typeahead);
		$fetchForm.submit();
	};

	$('.for-check-all').append(
		$('<input />', { type: 'checkbox', 'class': 'check-all use-tooltip', title: 'Select all/none' })
	).find('input').change(function() {
		$(this).closest('table').selectDeselectCheckboxes(this);
	});

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
							.html('<i class="fa fa-ban"></i> Clear selected items')
							.on('click', function() {
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

		$list.closest('.student-filter').find('button.apply').addClass('btn-primary');
		updateClearAllButton($el);
	};

	var updateClearAllButton = function($el) {
		var $filterList = $el.closest(".student-filter");

		if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
			$filterList.find('.clear-all-filters').attr("disabled", "disabled");
		} else {
			$filterList.find('.clear-all-filters').removeAttr("disabled");
		}
	};

	$studentFilter.on('change', function(e) {
		var $input = $(e.target);
		if ($input.is('.prevent-reload')) return;
		updateFilter($input);
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

	$studentFilter.prepend(
		$('<button class="btn btn-xs btn-default clear-all-filters" type="submit" disabled>Clear filter</button>')
	);

	var $clearAllButtons = $('.clear-all-filters').on('click', function() {
		$(this).closest('.student-filter').find('.filter-list').each(function() {
			var $list = $(this);

			$list.find('input:checked').each(function() {
				var $checkbox = $(this);
				$checkbox.prop('checked', false);
				updateFilter($checkbox);
			});

			prependClearLink($list);
		});
	});

	$clearAllButtons.each(function() {
		updateClearAllButton($(this));
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

	var $previewForm = $('form.preview'), $uploadForm = $('#uploadCommand');
	$previewForm.areYouSure({
		'dirtyClass' : 'dirty'
	});
	if ($previewForm.find('input[name^=additions]').length > 0 || $previewForm.find('input[name^=removals]').length > 0) {
		$previewForm.addClass('dirty');
	}
	$fetchForm.on('submit', function(){
		$previewForm.removeClass('dirty');
	});
	$uploadForm.on('submit', function(){
		$previewForm.removeClass('dirty');
	});
	$uploadForm.find('.btn.dirty-check-ignore').on('click', function(){
		if ($previewForm.is('.dirty')) {
			$previewForm.removeClass('dirty');
			window.setTimeout(function() { // Wait for the unload event for the file request
				$previewForm.addClass('dirty');
			}, 100);
		}
	});

});