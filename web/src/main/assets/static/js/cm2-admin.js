(function ($) { "use strict";
	var exports = {};

	/**
	 *
	 * Valid options
	 *
	 * beforeSubmit - function run before the form is submitted
	 * errorCallback - callback when the form returns validation errors
	 * successCallback - callback when the form is submitted with no errors
	 * type - the type of this form
	 */
	$.fn.tabulaAjaxForm = function(options) {
		var $row = $(this);
		var $form = $row.find('form.ajax-form');
		var $container = $row.find('.detailrow-container');
		if($container.length === 0){
			$container = $('#content-'+$row.data('contentid'));
		}
		prepareAjaxForm($form, $container, options);
	};

	$.fn.bindFormHelpers = function() {
		var $this = $(this);
		// bind form helpers
		$this.find('input.date-time-picker').tabulaDateTimePicker();
		$this.find('input.date-picker').tabulaDatePicker();
		$this.find('input.time-picker').tabulaTimePicker();
		$this.find('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
		$this.find('form.double-submit-protection').tabulaSubmitOnce();
		$this.find('.use-popover').tabulaPopover({trigger: 'click', container: 'body'});
		$this.find('.table-sortable').sortableTable();
	};
	$(function(){
		var $body = $('body');
		// fixed header and footer
		$body.find('.fix-area').fixHeaderFooter();
		// sortable tables
		$body.find('.table-sortable').sortableTable();
		// expandable table rows
		$body.on('tabula.expandingTable.contentChanged',function(e){
			var $this = $(e.target);
			$this.bindFormHelpers();
			$this.trigger('tabula.formLoaded');
		});
		$body.on('show.bs.collapse', '.detail-row', function(e){
			var $this = $(e.target);
			var hasLoaded = $this.data('loaded') || false;
			if(!hasLoaded) {
				var detailUrl = $this.data('detailurl');

				var $content = $this.find('td');

				$this.data('request', $.ajax({
					url: detailUrl,
					statusCode: {
						403: function () {
							$content.html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
						}
					},
					success: function (data) {
						$content.html(data);
						// bind form helpers
						$this.bindFormHelpers();
						$this.data('loaded', true);
						$this.trigger('tabula.formLoaded');
					},
					error: function () {
						$content.html("<p>No data is currently available. Please check that you are signed in.</p>");
					}
				}));
			}
		});
		$('#feedback-adjustment').on('tabula.formLoaded', '.detail-row', function(e){

			var $content = $(e.target);

			// activate any popovers
			$content.find('.use-popover').tabulaPopover({
				trigger: 'click',
				container: 'body'
			});

			// bind suggested mark button
			$content.find('.use-suggested-mark').on('click', function(e){
				var $target = $(this);
				var $markInput = $content.find('input[name=adjustedMark]');
				var $commentsTextarea = $content.find('textarea[name=comments]');
				var mark = $content.find('button').attr("data-mark");
				var comment =$content.find('button').attr("data-comment");
				$markInput.val(mark);
				$commentsTextarea.val(comment);
				// simulate a keyup to trigger and grade validation
				$markInput.keyup();
				e.preventDefault();
			});

			var $select = $content.find('select[name=reason]');
			var $otherInput = $content.find('.other-input');
			$otherInput.addClass("hide");
			if ($otherInput.val() === "Late submission penalty") {
				$content.find('option[value="Late submission penalty"]').prop('selected', true);
			}
			if ($otherInput.val() === "Plagarism penalty") {
				$content.find('option[value="Plagarism penalty"]').prop('selected', true);
			}
			if ($otherInput.val() !== "Plagarism penalty" && $otherInput.val() !== "Late submission penalty" && $otherInput.val() !== "") {
				$content.find('option[value=Other]').prop('selected', true);
				$otherInput.prop("disabled", false);
				$otherInput.removeClass("hide");
			}
			// show the suggested mark button if late penalty is selected
			var $suggestedPenalty = $select.closest('form').find('.late-penalty');
			if($select.val() === "Late submission penalty") {
				$suggestedPenalty.removeClass("hide");
			} else {
				$suggestedPenalty.addClass("hide");
			}
		});

		$body.on('change', 'select[name=reason]', function(e){
			var $target = $(e.target);
			var $otherInput = $target.siblings('.other-input');
			if ($target.find('option:selected').text() === "Other") {
				$otherInput.prop("disabled", false);
				$otherInput.removeClass("hide");
				$otherInput.fadeIn(400);
			} else if ($otherInput.is(':visible')){
				$otherInput.fadeOut(400, function() {
					$otherInput.prop('disabled', true);
					$otherInput.addClass("hide");
				});
			}
			var $suggestedPenalty = $target.closest('form').find('.late-penalty');
			if ($target.val() === "Late submission penalty") {
				$suggestedPenalty.fadeIn(400);
				$suggestedPenalty.removeClass("hide");
			} else if ($suggestedPenalty.is(':visible')) {
				$suggestedPenalty.fadeOut(400);
				$suggestedPenalty.addClass("hide");
			}
		});

		// extension management - on submit replace html with validation errors or redirect
		$body.on('click', 'form.modify-extension button:submit', function () {
			$(this).closest('form').data('buttonClicked', {
				name: $(this).attr('name'),
				value: $(this).val()
			});
		});

		$body.on('submit', 'form.modify-extension', function(e){
			e.preventDefault();
			var $form = $(e.target);
			var $detailRow = $form.closest('.content-container, .detailrow-container');
			var formData = $form.serializeArray();

			var buttonClicked = $form.data('buttonClicked');
			if (buttonClicked) {
				formData.push(buttonClicked);
				$form.data('buttonClicked', null);
			}

			$.post({
				url: $form.attr('action'),
				data: formData,
				success: function (data) {
					if (data.success) {
						window.location.replace(data.redirect);
					} else {
						$detailRow.html(data);
						$detailRow.bindFormHelpers();
					}
				},
				error: function (xhr) {
					$detailRow.html(xhr.responseText);
				},
				complete: function () {
					$body.find('.expanding-table').trigger('tabula.expandingTable.repositionContent');
				}
			});
		});

		// handlers for online marking form

		// on cancel collapse the row and nuke the form
		$body.on('click', '.cancel', function(e){
			e.preventDefault();
			var $row = $(e.target).closest('.detail-row');
			$row.collapse("hide");

			$row.on('hidden.bs.collapse', function(e) {
				$row.data('loaded', false);
				$row.find('.detailrow-container').html('<i class="fa fa-spinner fa-spin"></i> Loading');
				$(this).unbind(e);
			});
		});

		// on reset fetch the form again
		$body.on('click', '.reset', function(e){
			e.preventDefault();
			var $row = $(e.target).closest('.detail-row');
			$row.data('loaded', false);
			$row.trigger('show.bs.collapse');
		});

		// remove attachment
		$body.on("click", '.remove-attachment', function(e) {
			e.preventDefault();
			var $this = $(this);
			var $form = $this.closest('form');
			var $li = $this.closest("li");
			$li.find('input, a').remove();
			$li.find('span').wrap('<del />');
			$li.find('i').css('display', 'none');
			var $ul = $li.closest('ul');

			if (!$ul.find('li').last().is('.pending-removal')) {
				var alertMarkup = '<li class="pending-removal">Files marked for removal won\'t be deleted until you <samp>Save</samp>.</li>';
				$ul.append(alertMarkup);
			}

			if($form.find('input[name=attachedFiles]').length === 0){
				var $blankInput = $('<input name="attachedFiles" type="hidden" />');
				$form.append($blankInput);
			}
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
				$('#open-reminder-dt').prop("disabled", false);
				$('#close-dt').prop("disabled", true);
			}  else {
				$('#close-dt').prop("disabled", false);
				$('#open-reminder-dt').prop("disabled", true);
			}
		});
		$('input#publishFeedback').change(function(){
			$('#dissertation-checkbox').prop('disabled', !$(this).is(':checked'));
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

	function setUpPaginationButtons() {
		$(function(){
			$('.pagination a').on('click', function (e) {
				e.preventDefault();
				e.stopPropagation();
				var pageNumber = $(this).data('page');
				if (location.search) {
					if (location.search.indexOf('page=') != -1) {
						location.search = location.search.replace(/page=\d+/, 'page=' + pageNumber)
					} else {
						location.search = location.search + '&page=' + pageNumber;
					}
				} else {
					location.search = '?page=' + pageNumber;
				}
			});
		});
	}
	setUpPaginationButtons();
	$(document).on('tabula.filterResultsChanged', setUpPaginationButtons);

	// code for bulk copy assignments
	$(function(){
		$('.copy-assignments').bigList({
			setup: function(e){
				if(!$(".collection-checkbox").is(":checked")){
					$('.btn-primary').prop('disabled', true);
				}
			},
			onSomeChecked: function() {
				$('.btn-primary').prop('disabled', false);
			},
			onNoneChecked: function() {
				$('.btn-primary').prop('disabled', true);
			}
		});
	});
	/**
	 * Helper for ajaxified forms in tabula expanding tables
	 *
	 * This glues together the ajaxForm and bootstrap collapsable plugins. It adds boilerplate for coping with
	 * signed-out users, and provides a callback for giving UI feedback after the form has been processed.
	 *
	 * callback() is passed the raw response text.
	 * It MUST return an empty string if the form has been successfully handled.
	 * Otherwise, whatever is returned will be rendered in the container.
	 */
	var prepareAjaxForm = function($form, $container, options) {
		options = options || {};

		var errorCallback = options.errorCallback || function(){};
		var successCallback = options.successCallback || function(){};
		var beforeSubmit = options.beforeSubmit || function() { return true; };
		var formType = options.type || "";
		$form.ajaxForm({
			beforeSerialize: beforeSubmit,
			beforeSubmit: function() {
				if(formType === 'feedback-adjustment') {
					if($("#action").val() != "suggestmark") {
						return true;
					}else{
						var $detailrow = $form.closest('.detail-row');
						$detailrow.find('.btn-primary').removeClass('disabled');
						$detailrow.find('.discard-changes').removeClass('disabled');
						$form.unbind('submit');
						$form.removeData('submitOnceSubmitted');
						$detailrow.trigger('tabula.formLoaded');
						return false;
					}
				}else{
					return true;
				}
			},
			iframe: true,
			statusCode: {
				403: function(jqXHR) {
					$container.html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
				}
			},
			success: function(response) {
				var result;

				if (response.indexOf('id="dev"') >= 0) {
					// for debugging freemarker...
					result = $(response).find('#column-1-content');
				} else {
					var $resp = $(response);
					// there should be an ajax-response class somewhere in the response text
					var $response = $resp.find('.ajax-response').addBack().filter('.ajax-response');
					var success = $response.length && $response.data('status') == 'success';
					if (success) {
						result =  "";
					} else {
						result =  response;
					}
				}
				var $detailrow = $container.closest('.detail-row');
				if (!result || /^\s*$/.test(result)) {
					// reset if empty
					$detailrow.collapse("hide");
					$detailrow.data('loaded', false);
					$container.html('<span class="text-muted"><i class="fa fa-spinner fa-spin"></i> Loading&hellip;</span>');
					successCallback($container);
				} else {
					$container.html(result);
					$detailrow.trigger('tabula.formLoaded');
					errorCallback($container);
				}
			},
			error: function(){ alert("There has been an error. Please reload and try again."); }
		});
	};

	/**
	 * Special case for downloading submissions as PDF.
	 * Does a check for non PDF files in submissions before fetching the download
	 */
	exports.wirePDFDownload = function ($scope) {
		$scope = $scope || $('body');

		$('a.download-pdf', $scope).on('click', function(e){
			var $this = $(this);
			e.preventDefault();
			if(!$this.hasClass("disabled") && $this.closest('.disabled').length === 0) {
				var $modal = $($this.data('target') || '#download-pdf-modal'), action = this.href;
				if ($this.data('href')) {
					action = $this.data('href')
				}
				var postData = $this.closest('div.form-post-container').find('.collection-checkbox:checked').map(function(){
					var $input = $(this);
					return $input.prop('name') + '=' + $input.val();
				}).get().join('&');
				$.post(action, postData, function(data){
					if (data.submissionsWithNonPDFs.length === 0) {
						// Use native click instead of trigger because there's no click handler for the marker version
						$modal.find('.btn.btn-primary').click();
					} else {
						$modal.find('.count').html(data.submissionsWithNonPDFs.length);
						var $submissionsContainer = $modal.find('.submissions').empty();
						$.each(data.submissionsWithNonPDFs, function(i, submission){
							var fileList = $('<ul/>');
							$.each(submission.nonPDFFiles, function(i, file){
								fileList.append($('<li/>').html(file));
							});
							$submissionsContainer.append($('<li/>').append(
								(submission.name.length > 0) ? submission.name + ' (' + submission.universityId + ')' : submission.universityId
							).append(fileList))
						});
						$modal.modal('show');
					}
				});
			}
		});
	};
	$(function () { exports.wirePDFDownload(); });

    exports.initBigList = function ($scope) {
		$scope = $scope || $('body');

		var biglistOptions = {
			setup: function() {
				var $container = this, $outerContainer = $container.closest('div.form-post-container');
				// #delete-selected-button won't work for >1 set of checkboxes on a page.
				$('#download-selected-button, #delete-selected-button', $outerContainer).click(function(event){
					event.preventDefault();

					var $checkedBoxes = $(".collection-checkbox:checked", $container);
					if ($container.data('checked') !== 'none') {
						var $form = $('<form />').attr({method:'POST',action:this.href}).hide();
						$form.append($checkedBoxes.clone());
						$(document.body).append($form);
						$form.submit();
					}
					return false;
				});

				function goToMarkPlagiarised(action, plagiarised, $checkedBoxes) {
					var $form = $('<form>').attr({method: 'POST', action: action}).hide();
					$form.append($checkedBoxes.clone());

					if (!plagiarised) {
						$form.append("<input type='hidden' name='markPlagiarised' value='false'>");
					}

					$(document.body).append($form);
					$form.submit();
				}


				$('#mark-plagiarised-selected-button:not(.disabled)', $outerContainer).click(function (e) {
					e.preventDefault();

					if ($container.data('checked') !== 'none' && !$(this).parent().hasClass('disabled')) {
						var $checkedBoxes = $(".collection-checkbox:checked", $container);
						goToMarkPlagiarised(this.href, true, $checkedBoxes);
					}

					return false;
				});

				$('#unmark-plagiarised-selected-button:not(.disabled)', $outerContainer).click(function (e) {
					e.preventDefault();

					if ($container.data('checked') !== 'none' && !$(this).parent().hasClass('disabled')) {
						var $checkedBoxes = $(".collection-checkbox:checked", $container);
						goToMarkPlagiarised(this.href, false, $checkedBoxes);
					}

					return false;
				});

				$('.form-post', $outerContainer).click(function(event){
					event.preventDefault();
					var $this = $(this);
					if(!$this.hasClass("disabled")) {
						var action = this.href;
						if ($this.data('href')) {
							action = $this.data('href')
						}

						var $form = $('<form />').attr({method: 'POST', action: action}).hide();
						var doFormSubmit = false;

						if ($container.data('checked') !== 'none' || $this.closest('.must-have-selected').length === 0) {
							var $checkedBoxes = $(".collection-checkbox:checked", $container);
							$form.append($checkedBoxes.clone());

							var $extraInputs = $(".post-field", $container);
							$form.append($extraInputs.clone());

							doFormSubmit = true;
						}

						if ($this.hasClass('include-filter') && ($('.filter-form').length > 0)) {
							var $inputs = $(':input', '.filter-form:not("#floatedHeaderContainer *")');
							$form.append($inputs.clone());

							doFormSubmit = true;
						}

						if (doFormSubmit) {
							$(document.body).append($form);
							$form.submit();
						} else {
							return false;
						}
					}
				});

				$('.form-post-single', $outerContainer).click(function(event){
					event.preventDefault();
					var $this = $(this);
					if(!$this.hasClass("disabled")) {
						var action = this.href;
						if ($this.data('href')) {
							action = $this.data('href')
						}

						var $form = $('<form />').attr({method: 'POST', action: action}).hide();
						var doFormSubmit = true;

						var $checkedBoxes = $(".collection-checkbox", $this.closest('itemContainer'));
						$form.append($checkedBoxes.clone().prop('checked', true));

						var $extraInputs = $(".post-field", $this.closest('itemContainer'));
						$form.append($extraInputs.clone());

						if (doFormSubmit) {
							$(document.body).append($form);
							$form.submit();
						} else {
							return false;
						}
					}
				});
			},

			onBulkChange: function ($checkboxes) {
				var $checkedBoxes = $checkboxes.filter(':checked');

				var plagiarisedCount = 0, notPlagiarisedCount = 0;

				$checkedBoxes.closest('tr').each(function () {
					if ($(this).data('plagiarised')) {
						plagiarisedCount++;
					} else {
						notPlagiarisedCount++;
					}
				});

				$('#mark-plagiarised-selected-button').parent().toggleClass('disabled', notPlagiarisedCount === 0);
				$('#unmark-plagiarised-selected-button').parent().toggleClass('disabled', plagiarisedCount === 0);
			}
		};

		$('.submission-feedback-list, .submission-list, .feedback-list, .marker-feedback-list, .coursework-progress-table, .expanding-table', $scope).not('.marker-feedback-table').bigList(
			$.extend(biglistOptions, {
				onSomeChecked : function() {
					$('.must-have-selected').removeClass('disabled');
				},

				onNoneChecked : function() {
					$('.must-have-selected').addClass('disabled');
				}
			})
		);
	};
	$(function () { exports.initBigList(); });

	exports.equalHeightTabContent = function ($scope) {
		$scope = $scope || $('body');
		$scope.find('.tab-content-equal-height').each(function () {
			var $container = $(this);
			var maxHeight = 0;
			$container.find('.tab-pane').each(function () {
				var $pane = $(this);
				var height = $pane.css({ height: '' }).height();
				if ($pane.is('.active')) {
					maxHeight = (height > maxHeight) ? height : maxHeight;
				} else {
					$pane.addClass('active');
					maxHeight = (height > maxHeight) ? height : maxHeight;
					$pane.removeClass('active');
				}
			});
			$container.find('.tab-pane').height(maxHeight);
			$container.trigger('tabula:tabContentHeightChange');
		});
	};

	// take anything we've attached to "exports" and add it to the global "Courses"
	// we use extend() to add to any existing variable rather than clobber it
	window.Coursework = $.extend(window.Coursework, exports);
})(jQuery);
