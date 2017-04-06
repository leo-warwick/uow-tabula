(function ($) { "use strict";

	$.fn.bindFormHelpers = function() {
		var $this = $(this);

		// bind form helpers
		$this.find('input.date-time-picker').tabulaDateTimePicker();
		$this.find('input.date-picker').tabulaDatePicker();
		$this.find('input.time-picker').tabulaTimePicker();
		$this.find('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
		$this.find('form.double-submit-protection').tabulaSubmitOnce();
	};

	$(function(){

		var $body = $('body');

		// sortable tables
		$body.find('.table-sortable').sortableTable();

		// expandable table rows
		$body.on('tabula.expandingTable.contentChanged',function(e){
			var $this = $(e.target);
			$this.bindFormHelpers();
		});

		$body.on('show.bs.collapse', '.detail-row', function(e){
			var $this = $(e.target);
			var hasLoaded = $this.data('loaded') || false;
			if(!hasLoaded) {
				var detailUrl = $this.data('detailurl');
				$this.data('request', $.get(detailUrl, function(data) {
					$this.find('td').html(data);
					// bind form helpers
					$this.bindFormHelpers();
					$this.data('loaded', true);
				}));
			}
		});

		// extension managemet - on submit replace html with validation errors or redirect
		$body.on('submit', 'form.modify-extension', function(e){
			e.preventDefault();
			var $form = $(e.target);
			var $detailRow = $form.closest('.detailrow-container');

			var formData = $form.serializeArray();
			var $buttonClicked =  $(document.activeElement);
			formData.push({ name: $buttonClicked.attr('name'), value: $buttonClicked.val() });

			$.post($form.attr('action'), formData, function(data) {
				if (data.success) {
					window.location.replace(data.redirect);
				} else {
					$detailRow.html(data);
					$detailRow.bindFormHelpers();
				}
			});
		});

		$('table.expanding-row-pairs').each(function(){
			$(this).find('tbody tr').each(function(i){
				if (i % 2 === 0) {
					var $selectRow = $(this), $expandRow = $selectRow.next('tr');
					$selectRow.data('expandRow', $expandRow.remove()).find('td:first').addClass('can-expand').prepend(
						$('<i/>').addClass('fa fa-fw fa-caret-right')
					);
				}
			}).end().on('click', 'td.can-expand', function(){
				var $row = $(this).closest('tr');
				if ($row.is('.expanded')) {
					$row.removeClass('expanded').next('tr').remove().end()
						.find('td i.fa-caret-down').removeClass('fa-caret-down').addClass('fa-caret-right');
				} else {
					$row.addClass('expanded').after($row.data('expandRow'))
						.find('td i.fa-caret-right').removeClass('fa-caret-right').addClass('fa-caret-down');
					//Tooltip invocation for row expansion to trigger tooltip. Invoking at top level does not trigger tooltips.
					$('.use-tooltip').tooltip();
				}
			}).find('tr.expand td.can-expand').trigger('click');
		});

		// code for tabs
		$('.nav.nav-tabs a').click(function (e) {
			e.preventDefault();
			$(this).tab('show');
		});

		$('input#openEnded').change(function(){
			var $this = $(this);
			if ($this.is(':checked'))  {
				$('#open-reminder-dt').removeAttr("disabled");
				$('#close-dt').attr("disabled", "disabled");
			}  else {
				$('#close-dt').removeAttr("disabled");
				$('#open-reminder-dt').attr("disabled","disabled");

			}
		});

		// check that the extension UI elements are present
		if($('input#allowExtensionRequests').length > 0){
			$('input#allowExtensionRequests').slideMoreOptions($('#request-extension-fields'), true);
		}


		var $assignmentpicker = $('.assignment-picker-input');
		var $assignmentQuery = $assignmentpicker.find('input[name=query]').attr('autocomplete', 'off');
		var target = $assignmentpicker.attr('data-target');
		$assignmentQuery.bootstrap3Typeahead({
			source: function(query, process) {
				if (self.currentSearch) {
					self.currentSearch.abort();
					self.currentSearch = null;
				}
				query = $.trim(query);
				self.currentSearch = $.ajax({
					url: $('.assignment-picker-input').attr('data-target'),
					data: {query: query},
					success: function(data) {
						var assignments = [];
						$.each(data, function(i, assignment) {
							var item = assignment.name + '|' + assignment.moduleCode + '|' + assignment.id;
							assignments.push(item);
						});
						process(assignments);
					}
				})
			},
			matcher: function(item) {
				return true;
			},
			sorter: function(items) {
				return items;
			}, // use 'as-returned' sort
			highlighter: function(item) {
				var assignment = item.split('|');
				return '<div class="description">' + assignment[0] + '-' + assignment[1] + '</div>';
			},
			updater: function(item) {
				var assignment = item.split('|');
				var assignmentId = assignment[2];
				$("#prefillAssignment").val(assignmentId);
				$assignmentpicker.val(assignmentId);
				$('#action-submit').val('');
				$("#command").submit();
				return assignment[0] + '-' + assignment[1];
			}
		});
	});

	var $assessmentComponentTable = $(".assessment-component table");
	$assessmentComponentTable.tablesorter({
		headers: {0:{sorter:false}}
	});


	// modals use ajax to retrieve their contents

	$(function() {

		$('body').on('submit', 'a[data-toggle=modal]', function(e){

			e.preventDefault();

			var $this = $(this);

			var $target = $($this.data('target'));

			var url = $this.attr('href');

			$target.load(url, function(){

				$target.bindFormHelpers()

			});

		});

		$("#modal-container").on("submit","input[type='submit']", function(e){

			e.preventDefault();

			var $this = $(this);

			var $form = $this.closest("form").trigger('tabula.ajaxSubmit');

			$form.removeClass('dirty');

			var updateTargetId = $this.data("update-target");

			var randomNumber = Math.floor(Math.random() * 10000000);

			jQuery.post($form.attr('action') + "?rand=" + randomNumber, $form.serialize(), function(data){
				window.location = data.result;
			});

		});

	});

})(jQuery);